package com.prep.kafka.producer;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

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
        return Math.abs(tenantKey.hashCode()) % numPartitions;
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
