package com.hello.suripu.logsindexer.settings;


import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentedBulkProcessorListener implements BulkProcessor.Listener{

    private final static Logger LOGGER = LoggerFactory.getLogger(InstrumentedBulkProcessorListener.class);

    private final Timer bulkTimer;
    private final Meter documentOutgoingMeter;
    private Timer.Context context;

    public InstrumentedBulkProcessorListener(final Timer bulkTimer, final Meter documentOutgoingMeter) {
        this.bulkTimer = bulkTimer;
        this.documentOutgoingMeter = documentOutgoingMeter;
    }
    public void beforeBulk(long executionId, BulkRequest request) {
        LOGGER.trace("Prepared !");
        context = bulkTimer.time();
    }

    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
        final String lastIndex =  response.getItems().length > 0 ? response.getItems()[response.getItems().length - 1].getIndex() : "";
        LOGGER.info(
                "Successfully bulk-processed {} documents from {} requests, last index used was {}",
                response.getItems().length,
                request.requests().size(),
                lastIndex
        );
        documentOutgoingMeter.mark(response.getItems().length);
        context.stop();
    }

    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
        LOGGER.error("Failed because {} !", failure.getMessage());
        context.stop();
    }
}
