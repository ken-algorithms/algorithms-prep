package com.prep.kafka.producer;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;

/**
 * Partitioner chia theo batch_id (UUID cua request).
 * GIAI PHAP FIX CHO HOTSPOT:
 * Do bai toan Event Counting chi can tong hop so dem theo khoang thoi gian ma KHONG can
 * thu tu tuyet doi giua cac request cua cung mot tenant, viec hash theo batch_id se
 * phan bo tai dong deu 100% tren moi partition, dong thoi retry cua cung 1 batch_id
 * van luon roi vao cung 1 partition giup stream worker dedup cuc bo re hon rat nhieu.
 */
public class BatchIdPartitioner implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        int numPartitions = cluster.partitionCountForTopic(topic);
        if (numPartitions <= 0) return 0;
        String batchKey = (key != null) ? key.toString() : "";
        return Math.abs(batchKey.hashCode()) % numPartitions;
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
