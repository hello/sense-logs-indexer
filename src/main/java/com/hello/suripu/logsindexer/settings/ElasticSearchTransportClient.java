package com.hello.suripu.logsindexer.settings;


import com.hello.suripu.logsindexer.configuration.ElasticSearchConfiguration;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

public class ElasticSearchTransportClient {

    public static TransportClient create(final ElasticSearchConfiguration elasticSearchConfiguration) {
        final Settings settings = ImmutableSettings.settingsBuilder()
                .put("transport.type", "org.elasticsearch.transport.netty.FoundNettyTransport")
                .put("transport.found.api-key", elasticSearchConfiguration.getApiKey())
                .put("cluster.name", elasticSearchConfiguration.getCluster())
                .put("client.transport.ignore_cluster_name", false)
                .put("client.transport.nodes_sampler_interval", elasticSearchConfiguration.getNodesSamplerInternval())
                .put("client.transport.ping_timeout", elasticSearchConfiguration.getPingTimeout())

                .build();
        return new TransportClient(settings).addTransportAddress(
                new InetSocketTransportAddress(elasticSearchConfiguration.getHost(), elasticSearchConfiguration.getTcpPort())
        );
    }
}
