package com.hello.suripu.logsindexer.settings;


import com.hello.suripu.logsindexer.configuration.ElasticSearchConfiguration;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.shield.ShieldPlugin;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ElasticSearchTransportClient {

    public static TransportClient create(final ElasticSearchConfiguration elasticSearchConfiguration) {
        final Settings settings = Settings.settingsBuilder()
                .put("shield.user","worker:" + elasticSearchConfiguration.getApiKey())
                .put("shield.transport.ssl", true)
                .put("action.bulk.compress", false)
                .put("cluster.name", elasticSearchConfiguration.getCluster())
                .put("request.headers.X-Found-Cluster", "${cluster.name}")
                .put("transport.ping_schedule", "5s")
                .build();
        try {
            final TransportAddress address =
                    new InetSocketTransportAddress(
                            InetAddress.getByName(elasticSearchConfiguration.getHost()),
                            elasticSearchConfiguration.getTcpPort()
                    );
            final TransportClient client = TransportClient.builder().
                    addPlugin(ShieldPlugin.class)
                    .settings(settings)
                    .build()
                    .addTransportAddress(address);
            return client;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
