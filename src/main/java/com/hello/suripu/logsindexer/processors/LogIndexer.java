package com.hello.suripu.logsindexer.processors;

import com.hello.suripu.api.logging.LoggingProtos;


public interface LogIndexer {
    int index(final LoggingProtos.BatchLogMessage t);
}

