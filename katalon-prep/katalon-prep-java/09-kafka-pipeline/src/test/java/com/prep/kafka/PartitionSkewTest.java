package com.prep.kafka;

import com.prep.kafka.producer.BatchIdPartitioner;
import com.prep.kafka.producer.TenantKeyPartitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartitionSkewTest {

    private static final String TOPIC = "events.test";
    private static final int NUM_PARTITIONS = 4;

    private final Cluster cluster = new Cluster(
            "test-cluster",
            List.of(new Node(0, "localhost", 9092)),
            List.of(
                    new PartitionInfo(TOPIC, 0, null, null, null),
                    new PartitionInfo(TOPIC, 1, null, null, null),
                    new PartitionInfo(TOPIC, 2, null, null, null),
                    new PartitionInfo(TOPIC, 3, null, null, null)
            ),
            Set.of(),
            Set.of()
    );

    @Test
    @DisplayName("Lỗi Skew: Key bằng tenant_id khiến Big Tenant làm lệch 1 partition")
    void testTenantKeyCausesPartitionSkew() {
        TenantKeyPartitioner partitioner = new TenantKeyPartitioner();
        Map<Integer, Integer> distribution = new HashMap<>();

        // Giả lập 10,000 request:
        // - Tenant A (Big Tenant) chiếm 40% (4,000 requests)
        // - 100 tenants nhỏ còn lại chia đều 60% (6,000 requests)
        for (int i = 0; i < 4_000; i++) {
            int p = partitioner.partition(TOPIC, "big_tenant_a", null, null, null, cluster);
            distribution.merge(p, 1, Integer::sum);
        }
        for (int i = 0; i < 6_000; i++) {
            String tenant = "small_tenant_" + (i % 100);
            int p = partitioner.partition(TOPIC, tenant, null, null, null, cluster);
            distribution.merge(p, 1, Integer::sum);
        }

        // Tối thiểu 1 partition phải gánh >= 4,000 requests (chiếm >= 40% tổng tải)
        int maxPartitionLoad = distribution.values().stream().mapToInt(v -> v).max().orElse(0);
        assertTrue(maxPartitionLoad >= 4000, "Partition chứa Big Tenant phải bị lệch tải nghiêm trọng");

        System.out.printf("=== Skew distribution (tenant_id key): %s (Max partition: %d/10000 = %.1f%%)%n",
                distribution, maxPartitionLoad, (maxPartitionLoad * 100.0 / 10000));
    }

    @Test
    @DisplayName("Sửa lỗi: Key bằng batch_id chia đều 100% tải lên mọi partition")
    void testBatchIdKeyProvidesBalancedDistribution() {
        BatchIdPartitioner partitioner = new BatchIdPartitioner();
        Map<Integer, Integer> distribution = new HashMap<>();

        // Cùng kịch bản 10,000 requests nhưng key bằng batch_id (UUID ngẫu nhiên)
        for (int i = 0; i < 10_000; i++) {
            String batchId = UUID.randomUUID().toString();
            int p = partitioner.partition(TOPIC, batchId, null, null, null, cluster);
            distribution.merge(p, 1, Integer::sum);
        }

        // Với 4 partition, mỗi partition kỳ vọng 25% = 2,500 requests
        // Kiểm tra mỗi partition nằm trong khoảng 2,200 - 2,800 (sai số nhỏ)
        for (int p = 0; p < NUM_PARTITIONS; p++) {
            int load = distribution.getOrDefault(p, 0);
            assertTrue(load >= 2200 && load <= 2800,
                    "Partition " + p + " phải nhận tải đồng đều, nhưng nhận: " + load);
        }

        System.out.printf("=== Balanced distribution (batch_id key): %s (Mỗi partition ~25%%)%n", distribution);
    }
}
