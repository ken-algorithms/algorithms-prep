package com.prep.kafka;

import com.prep.kafka.resilience.ProducerRateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProducerRateLimitingTest {

    @Test
    @DisplayName("Backpressure: Token bucket chặn burst vượt ngưỡng để chống tràn buffer của Kafka Producer")
    void testRateLimiterRejectsExcessiveBurst() {
        // Dung lượng burst = 10 tokens, hồi 2 tokens/giây
        ProducerRateLimiter limiter = new ProducerRateLimiter(10, 2.0);
        String tenant = "tenant_bursty";

        // 10 request đầu tiên phải được chấp nhận
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.tryAcquire(tenant), "Request thứ " + (i + 1) + " phải thành công");
        }

        // Request thứ 11 vượt quá burst quota -> bị từ chối ngay lập tức (trả về 429)
        assertFalse(limiter.tryAcquire(tenant), "Request thứ 11 phải bị từ chối để bảo vệ Kafka buffer");

        System.out.println("=== Rate limiter successfully guarded Kafka producer buffer against burst overload!");
    }
}
