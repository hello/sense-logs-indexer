package com.logsindexer.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ElasticSearchConfiguration {
    @Valid
    @NotNull
    @JsonProperty("host")
    private String host;
    public String getHost() {
        return host;
    }

    @Valid
    @NotNull
    @JsonProperty("cluster")
    private String cluster;
    public String getCluster() {
        return cluster;
    }

    @Valid
    @NotNull
    @JsonProperty("api_key")
    private String apiKey;
    public String getApiKey() {
        return apiKey;
    }

    @Valid
    @NotNull
    @JsonProperty("http_port")
    private Integer httpPort;
    public Integer getHttpPort() {
        return httpPort;
    }

    @Valid
    @NotNull
    @JsonProperty("tcp_port")
    private Integer tcpPort;
    public Integer getTcpPort() { return tcpPort; }

    @Valid
    @NotNull
    @JsonProperty("nodes_sampler_interval")
    private String nodesSamplerInternval;
    public String getNodesSamplerInternval() {
        return nodesSamplerInternval;
    }

    @Valid
    @NotNull
    @JsonProperty("ping_timeout")
    private String pingTimeout;
    public String getPingTimeout() {
        return pingTimeout;
    }

    @Valid
    @NotNull
    @JsonProperty("index_prefix")
    private String indexPrefix;
    public String getIndexPrefix() {
        return indexPrefix;
    }

    @Valid
    @NotNull
    @JsonProperty("max_bulk_actions")
    private Integer maxBulkActions;
    public Integer getMaxBulkActions() {
        return maxBulkActions;
    }

    @Valid
    @NotNull
    @JsonProperty("max_bulk_size_mb")
    private Integer maxBulkSizeMb;
    public Integer getMaxBulkSizeMb() {
        return maxBulkSizeMb;
    }
}
