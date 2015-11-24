package com.hello.suripu.logsindexer.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Sets;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.logsindexer.configuration.ElasticSearchConfiguration;
import com.hello.suripu.logsindexer.models.SenseDocument;
import com.hello.suripu.logsindexer.settings.ElasticSearchIndexMappings;
import com.hello.suripu.logsindexer.settings.ElasticSearchIndexSettings;
import com.hello.suripu.logsindexer.settings.InstrumentedBulkProcessorListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.transport.ConnectTransportException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Set;

import static com.codahale.metrics.MetricRegistry.name;


public class ElasticSearchIndexer implements LogIndexer {

    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticSearchIndexer.class);
    private static final int MAX_INDEX_CREATION_ATTEMPTS = 5;
    private static final int CERTIFIED_FIRMWARE_UPDATE_PERIOD_MINUTES = 5;
    private static final String CERTIFIED_FIRMWARE_SET_KEY = "certified_firmware";


    private final TransportClient transportClient;
    private final ElasticSearchConfiguration elasticSearchConfiguration;
    private final JedisPool jedisPool;

    private final Meter documentIncomingMeter;
    private final Meter documentOutgoingMeter;
    private final Timer bulkTimer;

    private DateTime lastCertifiedFirmwareUpdated;
    private Integer certifiedFirmwareUpdateCount;
    private Set<String> certifiedCombinedFirmwareVersions;

    private BulkProcessor bulkProcessor;
    private Set<String> allIndexes;


    public ElasticSearchIndexer (final TransportClient transportClient, final ElasticSearchConfiguration elasticSearchConfiguration, final JedisPool jedisPool, final MetricRegistry metricRegistry) {
        this.transportClient = transportClient;
        this.elasticSearchConfiguration = elasticSearchConfiguration;
        this.jedisPool = jedisPool;


        documentIncomingMeter = metricRegistry.meter(name(ElasticSearchProcessor.class, "document-incoming"));
        documentOutgoingMeter = metricRegistry.meter(name(ElasticSearchProcessor.class, "document-outgoing"));
        bulkTimer = metricRegistry.timer(name(ElasticSearchProcessor.class, "bulk-process-time"));

        lastCertifiedFirmwareUpdated = DateTime.now(DateTimeZone.UTC);
        certifiedFirmwareUpdateCount = 0;
        allIndexes = getAllIndexes(transportClient);

        final BulkProcessor.Listener instrumentedListener = new InstrumentedBulkProcessorListener(bulkTimer, documentOutgoingMeter);
        bulkProcessor = BulkProcessor.builder(transportClient, instrumentedListener)
                .setBulkActions(elasticSearchConfiguration.getMaxBulkActions())
                .setBulkSize(new ByteSizeValue(elasticSearchConfiguration.getMaxBulkSizeMb(), ByteSizeUnit.MB))
                .setConcurrentRequests(elasticSearchConfiguration.getBulkConcurrentRequests())
                .build();
    }


    public int index(final LoggingProtos.BatchLogMessage batchLogMessage) {
        final String currentIndex = determineIndexName(batchLogMessage);
        return addSenseLogDocumentToBulk(currentIndex, batchLogMessage);
    }

    private Set<String> getAllIndexes(final TransportClient client) {
        return Sets.newHashSet(client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData().concreteAllIndices());
    }

    private String determineIndexName(final LoggingProtos.BatchLogMessage batchLogMessage) {
        if (!batchLogMessage.hasReceivedAt() || batchLogMessage.getReceivedAt() == 0) {
            LOGGER.error("Batch log message does not have receivedAt time");
            return elasticSearchConfiguration.getFallbackIndex();
        }

        final String indexName = elasticSearchConfiguration.getIndexPrefix() +
                new DateTime(batchLogMessage.getReceivedAt(), DateTimeZone.UTC).toString(DateTimeFormat.forPattern("yyyy-MM-dd"));

        if (allIndexes.contains(indexName)) {
            return indexName;
        }

        final Boolean indexCreated = createIndex(indexName); // Create new index with custom settings and mappings on the fly

        if (indexCreated){
            allIndexes.add(indexName); // Update indexes set
            return indexName;
        }
        return elasticSearchConfiguration.getFallbackIndex();
    }

    private Boolean createIndex(final String indexName) {
        LOGGER.info("Prepare to create index {}", indexName);
        final CreateIndexRequest createIndexRequest = new CreateIndexRequest(
                indexName,
                ImmutableSettings.settingsBuilder().loadFromSource(ElasticSearchIndexSettings.createDefault().toJSONString().get()).build()
        ).mapping(ElasticSearchIndexMappings.DEFAULT_KEY, ElasticSearchIndexMappings.createDefault().get());

        for (int k = 0; k < MAX_INDEX_CREATION_ATTEMPTS ; k++) {
            try {
                final CreateIndexResponse createIndexResponse = transportClient.admin().indices().create(createIndexRequest).actionGet();
                LOGGER.info("Index {} created - {}", indexName, createIndexResponse.isAcknowledged());
                return true;
            } catch (final IndexAlreadyExistsException iaee) {
                LOGGER.warn("Index {} already existed", indexName);
                return true;
            } catch (final ConnectTransportException cte) {
                LOGGER.warn("Failed to create index {} because of a connection error", indexName);
            } catch (final Exception e) {
                LOGGER.warn("Failed to create index {} because {}", e.getMessage());
            }
            LOGGER.warn("Attempt {} to create index {}", k, indexName);
        }
        return false;
    }

    private int addSenseLogDocumentToBulk(final String indexName, final LoggingProtos.BatchLogMessage batchLogMessage) {
        documentIncomingMeter.mark(batchLogMessage.getMessagesCount());
        for(final LoggingProtos.LogMessage log : batchLogMessage.getMessagesList()) {
            final Long timestamp = (log.getTs() == 0) ? batchLogMessage.getReceivedAt() : log.getTs() * 1000L;
            final SenseDocument senseDocument = SenseDocument.create(log.getDeviceId(), timestamp, log.getMessage(), log.getOrigin(), log.getTopFwVersion(), log.getMiddleFwVersion(), getCertifiedCombinedFirmwareVersions());
            bulkProcessor.add(new IndexRequest(indexName, SenseDocument.DEFAULT_CATEGORY).source(senseDocument.toMap()));

            if (senseDocument.hasFirmwareCrash()){
                bulkProcessor.add(new IndexRequest(elasticSearchConfiguration.getFwCrashIndex(), SenseDocument.DEFAULT_CATEGORY).source(senseDocument.toMap()));
            }
        }
        return batchLogMessage.getMessagesCount();
    }

    private Set<String> getCertifiedCombinedFirmwareVersions() {
        if (lastCertifiedFirmwareUpdated.plusMinutes(CERTIFIED_FIRMWARE_UPDATE_PERIOD_MINUTES).isBeforeNow()
                || certifiedFirmwareUpdateCount == 0){
            lastCertifiedFirmwareUpdated = DateTime.now(DateTimeZone.UTC);
            Jedis jedis = null;
            String exceptionMessage = "";
            try {
                jedis = jedisPool.getResource();
                certifiedCombinedFirmwareVersions = jedis.smembers(CERTIFIED_FIRMWARE_SET_KEY);
                LOGGER.info("Updated set of certified combined firmware versions");
            } catch (JedisDataException e) {
                exceptionMessage = String.format("Failed to get data from redis -  %s", e.getMessage());
                LOGGER.error(exceptionMessage);
            } catch (Exception e) {
                exceptionMessage = String.format("Failed to update set of certified combined firmware versions because %s", e.getMessage());
                LOGGER.error(exceptionMessage);
            } finally {
                if (jedis != null) {
                    try {
                        if (exceptionMessage.isEmpty()) {
                            jedisPool.returnResource(jedis);
                        } else {
                            jedisPool.returnBrokenResource(jedis);
                        }
                    } catch (JedisException e) {
                        LOGGER.error("Failed to return to resource {}", e.getMessage());
                    }

                }
            }
        }
        certifiedFirmwareUpdateCount += 1;
        return certifiedCombinedFirmwareVersions;
    }
}