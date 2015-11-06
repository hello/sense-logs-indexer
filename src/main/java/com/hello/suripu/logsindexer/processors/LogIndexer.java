package com.hello.suripu.logsindexer.processors;

import com.hello.suripu.api.logging.LoggingProtos;


public interface LogIndexer {
    void index(final LoggingProtos.BatchLogMessage t);
}

