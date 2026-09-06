package com.prep.quarkus.domain;

public enum ResultStatus {
    PASSED,
    FAILED,
    SKIPPED,
    /** Pass sau khi retry. KHONG duoc gop vao PASSED - xem module 01/PATTERNS.md. */
    FLAKY
}
