# sense-logs-indexer
- Index sense logs into elastic search documents via transport client.
- Server avaiable at https://5ef1fe1be448f837d91a38960abf6c0a.us-east-1.aws.found.io, ports are 9243 for HTTP and 9343 for TCP.
- Number of shards (2) and number of replicas (2) are set server-side, not exposed in this worker.
- Must run on java 8.
