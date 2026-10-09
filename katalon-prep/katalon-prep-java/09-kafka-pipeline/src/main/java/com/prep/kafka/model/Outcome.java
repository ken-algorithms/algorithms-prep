package com.prep.kafka.model;

/**
 * 4 ket qua phan loai su kien theo dung de bai Katalon Event Counting:
 * - TRUE: key hop le, gia tri true
 * - FALSE: key hop le, gia tri false
 * - FAKE_KEY: key khong nam trong registry
 * - MALFORMED: du lieu loi / thieu truong
 */
public enum Outcome {
    TRUE,
    FALSE,
    FAKE_KEY,
    MALFORMED
}
