package com.logsindexer.configuration;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class GraphiteConfiguration extends Configuration {
    @Valid
    @NotNull
    @JsonProperty("host")
    private String host;

    public String getHost() {
        return host;
    }

    @Valid
    @NotNull
    @JsonProperty("api_key")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    @Valid
    @Min(5)
    @Max(120)
    @JsonProperty("reporting_interval_in_seconds")
    private Integer reportingIntervalInSeconds;

    public Integer getReportingIntervalInSeconds() {
        return reportingIntervalInSeconds;
    }

}
