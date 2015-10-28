package com.hello.suripu.logsindexer.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;


public class KinesisConfiguration {

    @Valid
    @NotNull
    @JsonProperty("endpoint")
    private String endpoint;

    public String getEndpoint() {
        return endpoint;
    }


    @Valid
    @NotNull
    @JsonProperty("stream")
    private String stream;

    public String getStream() {
        return stream;
    }
}
