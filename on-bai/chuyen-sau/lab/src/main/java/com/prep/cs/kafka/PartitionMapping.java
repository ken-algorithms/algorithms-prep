package com.prep.cs.kafka;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.common.utils.Utils;

/**
 * Producer mac dinh chon partition cho record CO key: toPositive(murmur2(key)) % soPartition.
 * Tang so partition thi phep chia doi: bao nhieu key doi partition, va vi sao thu tu theo key bi vo.
 *
 *   scripts/run.sh com.prep.cs.kafka.PartitionMapping
 */
public final class PartitionMapping {

    static int partitionFor(String key, int partitions) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        return Utils.toPositive(Utils.murmur2(bytes)) % partitions; // giong BuiltInPartitioner.partitionForKey
    }

    public static void main(String[] a) {
        int keys = 100_000;
        System.out.printf("%d key dang 'account-<n>'%n", keys);
        for (int[] change : new int[][]{{6, 7}, {6, 8}, {6, 12}, {12, 24}}) {
            int moved = 0;
            int[] load = new int[change[1]];
            for (int i = 0; i < keys; i++) {
                String k = "account-" + i;
                int before = partitionFor(k, change[0]), after = partitionFor(k, change[1]);
                if (before != after) moved++;
                load[after]++;
            }
            int min = java.util.Arrays.stream(load).min().orElseThrow(), max = java.util.Arrays.stream(load).max().orElseThrow();
            System.out.printf("%2d -> %2d partition: %5.1f%% key doi partition; tai sau: min %d, max %d%n",
                    change[0], change[1], 100.0 * moved / keys, min, max);
        }
        System.out.println("vi du: account-42 o partition " + partitionFor("account-42", 6) + " (6 partition), "
                + partitionFor("account-42", 8) + " (8 partition)");
    }
}
