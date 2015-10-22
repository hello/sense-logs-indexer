package com.logsindexer.framework;

import com.logsindexer.processors.ElasticSearchCommand;
import com.logsindexer.configuration.SenseLogsConfiguration;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.joda.time.DateTimeZone;

import java.util.TimeZone;

public class SenseLogsIndexer extends Application<SenseLogsConfiguration> {


    public static void main( String[] args ) throws Exception
    {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateTimeZone.setDefault(DateTimeZone.UTC);
        new SenseLogsIndexer().run(args);
    }

    @Override
    public String getName() {
        return "sense-logs-indexer";
    }

    @Override
    public void initialize(Bootstrap<SenseLogsConfiguration> bootstrap) {
        bootstrap.addCommand(new ElasticSearchCommand("index", "Index sense logs into ES documents"));
    }

    @Override
    public void run(final SenseLogsConfiguration configuration,
                    final Environment environment) {
    }
}
