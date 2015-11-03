package com.hello.suripu.logsindexer.processors;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.logsindexer.configuration.ElasticSearchConfiguration;
import com.hello.suripu.logsindexer.models.SenseDocument;
import com.hello.suripu.logsindexer.settings.ElasticSearchIndexMappings;
import com.hello.suripu.logsindexer.settings.ElasticSearchIndexSettings;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;


public class ElasticSearchIndexer implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticSearchIndexer.class);

    private final TransportClient transportClient;
    private final ElasticSearchConfiguration elasticSearchConfiguration;

    private final Meter bulkSize;
    private Timer bulkTimer;

    private BulkProcessor bulkProcessor;
    private List<String> currentIndexes;


    public ElasticSearchIndexer(final TransportClient transportClient, final ElasticSearchConfiguration elasticSearchConfiguration, final MetricRegistry metricRegistry) {
        this.transportClient = transportClient;
        this.elasticSearchConfiguration = elasticSearchConfiguration;

        bulkSize = metricRegistry.meter(name(ElasticSearchIndexer.class, "bulk-items"));
        bulkTimer = metricRegistry.timer(name(ElasticSearchIndexer.class, "bulk-process-time"));
    }


    public void initialize(final String shardId) {
        currentIndexes = getCurrentIndexes(transportClient);
        bulkProcessor = BulkProcessor.builder(
                transportClient,
                new BulkProcessor.Listener() {
                    Timer.Context context;
                    public void beforeBulk(long executionId,
                                           BulkRequest request) {
                        LOGGER.debug("Prepared !");
                        context = bulkTimer.time();

                    }

                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          BulkResponse response) {

                        LOGGER.info("Successfully bulk-processed {} documents from {} requests !", response.getItems().length, request.requests().size());
                        bulkSize.mark(response.getItems().length);
                        context.stop();
                    }

                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          Throwable failure) {
                        LOGGER.error("Failed because {} !", failure.getMessage());
                        context.stop();
                    }

                })
                .setBulkActions(elasticSearchConfiguration.getMaxBulkActions())
                .setBulkSize(new ByteSizeValue(elasticSearchConfiguration.getMaxBulkSizeMb(), ByteSizeUnit.MB))
                .setConcurrentRequests(elasticSearchConfiguration.getBulkConcurrentRequests())
                .build();
    }

    public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
        for (final Record record : records) {
            try {
                final LoggingProtos.BatchLogMessage batchLogMessage = LoggingProtos.BatchLogMessage.parseFrom(record.getData().array());
                if(batchLogMessage.hasLogType()) {
                    if (!batchLogMessage.getLogType().equals(LoggingProtos.BatchLogMessage.LogType.SENSE_LOG)) {
                        LOGGER.trace("Skip because this is not sense logs");
                        continue;
                    }
                    for(final LoggingProtos.LogMessage log : batchLogMessage.getMessagesList()) {
                        final Long timestamp = (log.getTs() == 0) ? batchLogMessage.getReceivedAt() : log.getTs() * 1000L;

                        final String indexName = elasticSearchConfiguration.getIndexPrefix() + new DateTime(timestamp).toString(DateTimeFormat.forPattern("yyyy-MM-dd"));

                        if (!currentIndexes.contains(indexName)) {
                            createIndex(indexName); // Create new index with custom settings and mappings on the fly
                            currentIndexes = getCurrentIndexes(transportClient);  // Update current indexes list
                        }

                        bulkProcessor.add(new IndexRequest(indexName, SenseDocument.DEFAULT_CATEGORY).source(
                                new SenseDocument(log.getDeviceId(), timestamp, log.getMessage(), log.getOrigin(), log.getTopFwVersion(), log.getMiddleFwVersion()).toMap()));
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to parse protobuf because {}", e.getMessage());
            }
        }
    }

    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {
        LOGGER.info("shutdown at {} because {}", iRecordProcessorCheckpointer.toString(), shutdownReason);

        try {
            bulkProcessor.awaitClose(elasticSearchConfiguration.getBulkAwaitCloseSeconds(), TimeUnit.SECONDS);
            LOGGER.warn("Bulk will be closed in {} seconds", elasticSearchConfiguration.getBulkAwaitCloseSeconds());
        }
        catch (final InterruptedException ie) {
            LOGGER.error("Failed to close bulk processor because {}", ie.getMessage());
            bulkProcessor.close(); // force closing bulk
            LOGGER.info("Successfully forced closing bulk", ie.getMessage());
        }

        if(shutdownReason == ShutdownReason.TERMINATE) {
            LOGGER.warn("Going to checkpoint");
            try {
                iRecordProcessorCheckpointer.checkpoint();
                LOGGER.warn("Checkpointed successfully");
            } catch (InvalidStateException e) {
                LOGGER.error(e.getMessage());
            } catch (ShutdownException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    private List<String> getCurrentIndexes(final TransportClient client) {
        return Arrays.asList(client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData().concreteAllIndices());
    }


    private void createIndex(final String indexName) {
        LOGGER.info("Prepare to create index {}", indexName);
        final CreateIndexRequest createIndexRequest = new CreateIndexRequest(
                indexName,
                ImmutableSettings.settingsBuilder().loadFromSource(ElasticSearchIndexSettings.createDefault().toJSONString().get()).build()
        ).mapping(ElasticSearchIndexMappings.DEFAULT_KEY, ElasticSearchIndexMappings.createDefault().get());

        try {
            final CreateIndexResponse createIndexResponse = transportClient.admin().indices().create(createIndexRequest).actionGet();
            LOGGER.info("Index {} created - {}", indexName, createIndexResponse.isAcknowledged());
        } catch (final IndexAlreadyExistsException iaee) {
            LOGGER.warn("Index {} already existed", indexName);
        }
    }
}
