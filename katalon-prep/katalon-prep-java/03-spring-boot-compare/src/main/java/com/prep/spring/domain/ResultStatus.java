package com.prep.spring.domain;

public enum ResultStatus {
    PASSED,
    FAILED,
    SKIPPED,
    /** Pass sau khi retry. KHONG gop vao PASSED. */
    FLAKY
}
