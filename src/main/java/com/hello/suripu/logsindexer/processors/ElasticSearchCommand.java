package com.hello.suripu.logsindexer.processors;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.hello.suripu.logsindexer.configuration.SenseLogsConfiguration;
import com.hello.suripu.logsindexer.framework.SenseLogsCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;


public class ElasticSearchCommand extends SenseLogsCommand<SenseLogsConfiguration> {

    public ElasticSearchCommand(final String name, final String description) {
        super(name, description);
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseLogsConfiguration.class);

    @Override
    protected void run(Environment environment, Namespace namespace, SenseLogsConfiguration configuration) throws Exception {
        if(configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";
            final String prefix = String.format("%s.%s.logsindexer", apiKey, env);

            final Graphite graphite = new Graphite(new InetSocketAddress(graphiteHostName, 2003));

            final GraphiteReporter graphiteReporter = GraphiteReporter.forRegistry(environment.metrics())
                    .prefixedWith(prefix)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(MetricFilter.ALL)
                    .build(graphite);
            graphiteReporter.start(interval, TimeUnit.SECONDS);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

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
