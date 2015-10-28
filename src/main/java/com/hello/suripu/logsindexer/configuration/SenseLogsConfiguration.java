package com.hello.suripu.logsindexer.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

public class SenseLogsConfiguration extends Configuration {
    @Valid
    @JsonProperty("debug")
    private Boolean debug;

    public Boolean getDebug() { return debug; }


    @Valid
    @NotNull
    @JsonProperty("app_name")
    private String appName;

    public String getAppName() {
        return appName;
    }


    @Valid
    @NotNull
    @Max(500)
    @JsonProperty("max_records")
    private Integer maxRecords;

    public Integer getMaxRecords() {
        return maxRecords;
    }


    @Valid
    @JsonProperty("kinesis")
    private KinesisConfiguration kinesisConfiguration;

    public KinesisConfiguration getKinesisConfiguration() { return kinesisConfiguration; }


    @Valid
    @NotNull
    @JsonProperty("elastic_search")
    private ElasticSearchConfiguration elasticSearchConfiguration;

    public ElasticSearchConfiguration getElasticSearchConfiguration() {
        return elasticSearchConfiguration;
    }
}

