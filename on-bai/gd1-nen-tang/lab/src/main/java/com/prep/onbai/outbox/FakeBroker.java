package com.prep.onbai.outbox;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Broker gia (thay Kafka/SQS): ghi lai moi message nhan duoc; tiem loi duoc. At-least-once nhu broker that. */
public final class FakeBroker {

    public record Message(String topic, String key, String messageId, String payload) {}

    public static final class Unavailable extends RuntimeException {
        Unavailable() {
            super("broker khong san sang");
        }
    }

    private final List<Message> received = new CopyOnWriteArrayList<>();
    private final AtomicInteger failNext = new AtomicInteger();

    public void publish(String topic, String key, String messageId, String payload) {
        if (failNext.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            throw new Unavailable();
        }
        received.add(new Message(topic, key, messageId, payload));
    }

    public List<Message> received() {
        return received;
    }

    public void failNext(int times) {
        failNext.set(times);
    }
}
