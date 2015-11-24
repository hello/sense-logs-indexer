package com.hello.suripu.logsindexer.processors;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.codahale.metrics.MetricRegistry;
import com.hello.suripu.coredw8.configuration.RedisConfiguration;
import com.hello.suripu.logsindexer.configuration.ElasticSearchConfiguration;
import com.hello.suripu.logsindexer.settings.ElasticSearchTransportClient;
import redis.clients.jedis.JedisPool;


public class ElasticSearchIndexerFactory implements IRecordProcessorFactory {
    private final ElasticSearchConfiguration elasticSearchConfiguration;
    private final RedisConfiguration redisConfiguration;
    private final MetricRegistry metricRegistry;

    public ElasticSearchIndexerFactory(final ElasticSearchConfiguration elasticSearchConfiguration, final RedisConfiguration redisConfiguration, final MetricRegistry metricRegistry) {
        this.elasticSearchConfiguration = elasticSearchConfiguration;
        this.redisConfiguration = redisConfiguration;
        this.metricRegistry = metricRegistry;
    }

    public IRecordProcessor createProcessor() {
        return ElasticSearchProcessor.create(
                new ElasticSearchIndexer(
                        ElasticSearchTransportClient.create(elasticSearchConfiguration),
                        elasticSearchConfiguration,
                        new JedisPool(redisConfiguration.getHost(), redisConfiguration.getPort()),
                        metricRegistry
                )

        );
    }
}
