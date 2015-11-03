package com.hello.suripu.logsindexer.processors;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.codahale.metrics.MetricRegistry;
import com.hello.suripu.logsindexer.configuration.ElasticSearchConfiguration;
import com.hello.suripu.logsindexer.settings.ElasticSearchTransportClient;


public class ElasticSearchIndexerFactory implements IRecordProcessorFactory {
    private final ElasticSearchConfiguration elasticSearchConfiguration;
    private final MetricRegistry metricRegistry;

    public ElasticSearchIndexerFactory(final ElasticSearchConfiguration elasticSearchConfiguration, final MetricRegistry metricRegistry) {
        this.elasticSearchConfiguration = elasticSearchConfiguration;
        this.metricRegistry = metricRegistry;
    }
    public IRecordProcessor createProcessor() {
        return new ElasticSearchIndexer(
                ElasticSearchTransportClient.create(elasticSearchConfiguration),
                elasticSearchConfiguration,
                metricRegistry
        );
    }
}
