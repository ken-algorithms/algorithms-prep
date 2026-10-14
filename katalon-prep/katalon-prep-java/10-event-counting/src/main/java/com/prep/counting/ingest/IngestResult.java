package com.prep.counting.ingest;

/** What {@code POST /events} answers with status 202: received, newly stored, and ignored as retries. */
public record IngestResult(int received, int inserted, int duplicates) {}
