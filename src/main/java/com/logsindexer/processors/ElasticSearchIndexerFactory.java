package com.logsindexer.processors;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.logsindexer.configuration.ElasticSearchConfiguration;
import com.logsindexer.settings.ElasticSearchTransportClient;


public class ElasticSearchIndexerFactory implements IRecordProcessorFactory {
    private final ElasticSearchConfiguration elasticSearchConfiguration;
    public ElasticSearchIndexerFactory(final ElasticSearchConfiguration elasticSearchConfiguration) {
        this.elasticSearchConfiguration = elasticSearchConfiguration;
    }
    public IRecordProcessor createProcessor() {
        return new ElasticSearchIndexer(
                ElasticSearchTransportClient.create(elasticSearchConfiguration),
                elasticSearchConfiguration
        );
    }
}
