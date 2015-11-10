package com.hello.suripu.logsindexer.processors;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class ElasticSearchProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticSearchProcessor.class);

    private final  LogIndexer elasticSearchIndexer;

    private ElasticSearchProcessor(final  LogIndexer elasticSearchIndexer) {
        this.elasticSearchIndexer = elasticSearchIndexer;
    }

    public static ElasticSearchProcessor create(final LogIndexer elasticSearchIndexer) {
        return new ElasticSearchProcessor(elasticSearchIndexer);
    }

    public void initialize(final String shardId) {}

    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        for (final Record record : records) {
            try {
                final LoggingProtos.BatchLogMessage batchLogMessage = LoggingProtos.BatchLogMessage.parseFrom(record.getData().array());

                if(!batchLogMessage.hasLogType()) {
                    LOGGER.error("No log type detected");
                    continue;
                }

                if (!batchLogMessage.getLogType().equals(LoggingProtos.BatchLogMessage.LogType.SENSE_LOG)) {
                    LOGGER.trace("Skip because this is not sense logs");
                    continue;
                }

                int processedMessages = elasticSearchIndexer.index(batchLogMessage);
                LOGGER.trace(" Processed {} messages from batch {}", processedMessages);

            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to parse protobuf because {}", e.getMessage());
            }
        }

        try {
            iRecordProcessorCheckpointer.checkpoint();
            LOGGER.trace("Checkpointed {} records", records.size());
        } catch (final InvalidStateException ise) {
            LOGGER.error("Failed to check point because {}", ise.getMessage());
        } catch (final ShutdownException se) {
            LOGGER.error("Failed to check point because worker is shut down");
        }
    }

    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {
        LOGGER.info("shutdown at {} because {}", iRecordProcessorCheckpointer.toString(), shutdownReason);

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

}
