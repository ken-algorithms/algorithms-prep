package com.prep.kafka.producer;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.utils.Utils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Partitioner chia theo tenant_id.
 * BAT LOI: Khi mot tenant chiem 40% luong request (Big Tenant / Noisy Neighbour),
 * toan bo request cua tenant do do vao duy nhat 1 partition -> Gay Partition Hotspot / Skew.
 */
public class TenantKeyPartitioner implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        int numPartitions = cluster.partitionCountForTopic(topic);
        if (numPartitions <= 0) return 0;
        String tenantKey = (key != null) ? key.toString() : "";
        // Hash thuan theo tenantId
        // murmur2 + toPositive: cung cong thuc partitioner mac dinh cua Kafka cho record co key.
        // Math.abs(hashCode()) se ra so AM khi hashCode() == Integer.MIN_VALUE.
        byte[] bytes = keyBytes != null ? keyBytes : tenantKey.getBytes(StandardCharsets.UTF_8);
        return Utils.toPositive(Utils.murmur2(bytes)) % numPartitions;
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
