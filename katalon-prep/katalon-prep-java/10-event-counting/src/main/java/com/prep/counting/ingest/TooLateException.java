package com.prep.counting.ingest;

/** The batch is older than raw retention: it can no longer be counted correctly, so it is refused (HTTP 422). */
public class TooLateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TooLateException(String message) {
        super(message);
    }
}
