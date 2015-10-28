package com.hello.suripu.logsindexer.settings;


import com.google.common.base.Optional;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class ElasticSearchIndexMappings {
    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticSearchIndexMappings.class);
    public final static String DEFAULT_KEY = "_default_";

    public static Optional<XContentBuilder> createDefault() {
        try {
            return Optional.of(XContentFactory.jsonBuilder()
                    .startObject()
                    .startObject(DEFAULT_KEY)
                    .startObject("properties")
                    .startObject("text")
                    .field("type", "string")
                    .field("analyzer", ElasticSearchIndexSettings.DEFAULT_ANALYZER_NAME)
                    .endObject()
                    .startObject("@timestamp")
                    .field("type", "date")
                    .field("format", "dateOptionalTime")
                    .endObject()
                    .startObject("timestamp")
                    .field("type", "date")
                    .field("format", "dateOptionalTime")
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject());

        }catch (IOException ioe) {
            LOGGER.error(ioe.getMessage());
        }
        return Optional.absent();
    }
}
