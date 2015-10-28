package com.hello.suripu.logsindexer.processors;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.hello.suripu.logsindexer.configuration.SenseLogsConfiguration;
import com.hello.suripu.logsindexer.framework.SenseLogsCommand;

import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;

import java.net.InetAddress;


public class ElasticSearchCommand extends SenseLogsCommand<SenseLogsConfiguration> {

    public ElasticSearchCommand(final String name, final String description) {
        super(name, description);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, SenseLogsConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        final String workerId = InetAddress.getLocalHost().getCanonicalHostName();
        final KinesisClientLibConfiguration kinesisConfig = new KinesisClientLibConfiguration(
                configuration.getAppName(),
                configuration.getKinesisConfiguration().getStream(),
                awsCredentialsProvider,
                workerId);

        kinesisConfig.withInitialPositionInStream(InitialPositionInStream.LATEST);
        kinesisConfig.withMaxRecords(configuration.getMaxRecords());
        kinesisConfig.withKinesisEndpoint(configuration.getKinesisConfiguration().getEndpoint());
        kinesisConfig.withIdleTimeBetweenReadsInMillis(10000);

        final IRecordProcessorFactory processorFactory = new ElasticSearchIndexerFactory(
                configuration.getElasticSearchConfiguration()
        );

        final Worker kinesisWorker = new Worker(processorFactory, kinesisConfig);
        kinesisWorker.run();
    }
}
