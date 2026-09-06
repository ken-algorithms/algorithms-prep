package com.prep.refresher;

import java.time.Duration;
import java.util.List;

/**
 * Sealed hierarchy + record de luyen PATTERN MATCHING FOR SWITCH (final tu Java 21).
 *
 * <p>Vi sao model bang sealed interface thay vi enum + field nullable? Vi moi ket qua co DU LIEU
 * KHAC NHAU: Passed chi can duration, Failed can message + stack, Skipped can ly do, Flaky can so
 * lan retry. Neu dung 1 class voi tat ca field thi nua so field luon null -> moi cho dung phai
 * null-check, va compiler khong giup gi.
 *
 * <p>Voi sealed: compiler BIET het cac nhanh, nen switch khong can `default`. Them 1 loai ket qua
 * moi -> moi switch thieu nhanh se BAO LOI COMPILE thay vi chay sai luc runtime. Day chinh la
 * "exhaustiveness" - thu ma enum + if-else khong cho ban.
 *
 * <p>On-domain Katalon: day dung la model ket qua chay test case.
 */
public sealed interface TestResult {

    String testName();

    record Passed(String testName, Duration duration) implements TestResult {}

    record Failed(String testName, String message, List<String> stackTrace) implements TestResult {
        /**
         * Compact constructor: cho phep validate + normalize NGAY trong record.
         * Day la cho de "lam chat" invariant - record khong co nghia la khong duoc kiem tra.
         */
        public Failed {
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Failed result must carry a message");
            }
            // List.copyOf: chan caller giu reference roi sua list sau khi tao record (defensive copy).
            // Record chi immutable o REFERENCE, khong tu dong immutable o noi dung collection.
            stackTrace = List.copyOf(stackTrace);
        }
    }

    record Skipped(String testName, String reason) implements TestResult {}

    /** Flaky = pass sau khi retry. Katalon phai phan biet cai nay voi Passed thuc su. */
    record Flaky(String testName, int attempts, Duration duration) implements TestResult {
        public Flaky {
            if (attempts < 2) {
                throw new IllegalArgumentException("Flaky implies at least 2 attempts, got " + attempts);
            }
        }
    }
}
