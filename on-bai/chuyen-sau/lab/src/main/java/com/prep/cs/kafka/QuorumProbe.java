package com.prep.cs.kafka;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;

/**
 * Goi lan luot cac API cua Admin, moi cai timeout 8 giay. Chay khi quorum controller con du va khi da mat
 * quorum de thay API nao do BROKER tra loi tu metadata cache, API nao phai di toi controller.
 *
 *   scripts/run.sh com.prep.cs.kafka.QuorumProbe localhost:19092 q2
 */
public final class QuorumProbe {

    public static void main(String[] a) throws Exception {
        String bootstrap = a[0], topic = a[1];
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", bootstrap,
                "default.api.timeout.ms", 8_000, "request.timeout.ms", 8_000))) {
            call("describeTopics", () -> "leader=" + admin.describeTopics(List.of(topic)).allTopicNames().get()
                    .get(topic).partitions().get(0).leader().id());
            call("describeConfigs", () -> "min.insync.replicas=" + admin.describeConfigs(
                            List.of(new ConfigResource(ConfigResource.Type.TOPIC, topic))).all().get()
                    .values().iterator().next().get("min.insync.replicas").value());
            call("listPartitionReassignments", () -> admin.listPartitionReassignments().reassignments().get().toString());
            call("describeMetadataQuorum", () -> "leader=" + admin.describeMetadataQuorum().quorumInfo().get().leaderId());
            call("createTopics", () -> {
                admin.createTopics(List.of(new NewTopic(topic + "-new-" + System.currentTimeMillis() % 100_000, 1, (short) 3)))
                        .all().get();
                return "da tao";
            });
        }
    }

    private static void call(String name, Callable<String> c) {
        long t0 = System.nanoTime();
        try {
            String r = c.call();
            System.out.printf("%-28s OK   %-24s %5d ms%n", name, r, (System.nanoTime() - t0) / 1_000_000);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.out.printf("%-28s LOI  %-24s %5d ms%n", name, cause.getClass().getSimpleName(),
                    (System.nanoTime() - t0) / 1_000_000);
        }
    }
}
