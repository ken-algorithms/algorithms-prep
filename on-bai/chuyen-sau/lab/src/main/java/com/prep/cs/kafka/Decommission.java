package com.prep.cs.kafka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.LogDirDescription;
import org.apache.kafka.clients.admin.NewPartitionReassignment;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;

/**
 * Rut mot broker khoi cum (scale-in) bang Admin API, lam dung nhung gi kafka-reassign-partitions.sh
 * --execute --throttle roi --verify lam ben trong:
 *   1. tim MOI partition (ca topic noi bo) co replica tren broker can rut
 *   2. lap ke hoach: thay broker do bang broker con lai dang giu it replica nhat, thay tai cho de giu thu tu
 *      (replica dau tien = leader uu tien)
 *   3. dat throttle: 2 config topic (replica nao bi gioi han) + 2 config broker (bao nhieu byte/giay)
 *   4. alterPartitionReassignments, moi giay doc kich thuoc cac ban sao moi de thay toc do chep that
 *   5. xong thi GO throttle; quen buoc nay thi ve sau cac partition do dong bo cham mai mai
 *
 *   scripts/run.sh com.prep.cs.kafka.Decommission localhost:19092 4 5000000
 */
public final class Decommission {

    record Move(TopicPartition tp, List<Integer> from, List<Integer> to, int leader, long bytes) {
        int adding() {
            return to.stream().filter(b -> !from.contains(b)).findFirst().orElseThrow();
        }
    }

    public static void main(String[] a) throws Exception {
        String bs = a[0];
        int victim = Integer.parseInt(a[1]);
        long rate = Long.parseLong(a[2]);
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", bs))) {
            Set<Integer> live = new TreeSet<>();
            for (Node n : admin.describeCluster().nodes().get()) live.add(n.id());
            Set<Integer> keep = new TreeSet<>(live);
            keep.remove(victim);
            System.out.printf("broker dang song %s; rut broker %d, con lai %s%n", live, victim, keep);

            Set<String> names = admin.listTopics(new ListTopicsOptions().listInternal(true)).names().get();
            Map<String, TopicDescription> topics = new TreeMap<>(admin.describeTopics(names).allTopicNames().get());
            Map<Integer, Map<TopicPartition, Long>> sizes = sizes(admin, live);

            // 1-2. partition can chuyen va ke hoach
            Map<Integer, Integer> load = new HashMap<>();
            topics.values().forEach(td -> td.partitions().forEach(p -> p.replicas().forEach(r -> load.merge(r.id(), 1, Integer::sum))));
            List<Move> moves = new ArrayList<>();
            for (TopicDescription td : topics.values()) {
                for (TopicPartitionInfo p : td.partitions()) {
                    List<Integer> from = p.replicas().stream().map(Node::id).toList();
                    if (!from.contains(victim)) continue;
                    int pick = keep.stream().filter(b -> !from.contains(b))
                            .min(Comparator.comparingInt((Integer b) -> load.getOrDefault(b, 0)).thenComparing(b -> b))
                            .orElseThrow(() -> new IllegalStateException(td.name() + "-" + p.partition()
                                    + ": khong con broker nao de giu du " + from.size() + " ban sao"));
                    List<Integer> to = new ArrayList<>(from);
                    to.set(to.indexOf(victim), pick);
                    load.merge(pick, 1, Integer::sum);
                    int leader = p.leader() == null ? -1 : p.leader().id();
                    TopicPartition tp = new TopicPartition(td.name(), p.partition());
                    long bytes = sizes.getOrDefault(leader, Map.of()).getOrDefault(tp, 0L);  // co ban leader moi chep
                    moves.add(new Move(tp, from, to, leader, bytes));
                }
            }
            long total = moves.stream().mapToLong(Move::bytes).sum();
            System.out.printf("ke hoach: %d partition co replica tren broker %d, can chep %.1f MB%n",
                    moves.size(), victim, total / 1e6);
            for (Move m : moves) {
                System.out.printf("  %-18s %s -> %s  leader %d  %6.1f MB%n",
                        m.tp(), m.from(), m.to(), m.leader(), m.bytes() / 1e6);
            }
            Map<Integer, Long> out = new TreeMap<>(), in = new TreeMap<>();
            for (Move m : moves) {
                out.merge(m.leader(), m.bytes(), Long::sum);
                in.merge(m.adding(), m.bytes(), Long::sum);
            }
            System.out.println("byte phai di qua tung broker (throttle tinh theo broker):");
            for (int b : keep) {
                System.out.printf("  broker %d: gui di (lam leader) %6.1f MB, nhan ve %6.1f MB%n",
                        b, out.getOrDefault(b, 0L) / 1e6, in.getOrDefault(b, 0L) / 1e6);
            }

            // 3. throttle: topic noi replica nao bi gioi han, broker noi gioi han bao nhieu
            Map<String, List<String>> leaderSide = new TreeMap<>(), followerSide = new TreeMap<>();
            for (Move m : moves) {
                for (int r : m.from()) {
                    leaderSide.computeIfAbsent(m.tp().topic(), k -> new ArrayList<>()).add(m.tp().partition() + ":" + r);
                }
                followerSide.computeIfAbsent(m.tp().topic(), k -> new ArrayList<>()).add(m.tp().partition() + ":" + m.adding());
            }
            Map<ConfigResource, Collection<AlterConfigOp>> throttle = new LinkedHashMap<>();
            for (String t : leaderSide.keySet()) {
                throttle.put(new ConfigResource(ConfigResource.Type.TOPIC, t), List.of(
                        set("leader.replication.throttled.replicas", String.join(",", leaderSide.get(t))),
                        set("follower.replication.throttled.replicas", String.join(",", followerSide.get(t)))));
            }
            for (int b : live) {
                throttle.put(new ConfigResource(ConfigResource.Type.BROKER, Integer.toString(b)), List.of(
                        set("leader.replication.throttled.rate", Long.toString(rate)),
                        set("follower.replication.throttled.rate", Long.toString(rate))));
            }
            admin.incrementalAlterConfigs(throttle).all().get();
            System.out.printf("dat throttle %.0f MB/s tren broker %s; vi du topic grow:%n", rate / 1e6, live);
            System.out.println("  leader.replication.throttled.replicas   = " + String.join(",", leaderSide.getOrDefault("grow", List.of())));
            System.out.println("  follower.replication.throttled.replicas = " + String.join(",", followerSide.getOrDefault("grow", List.of())));

            // 4. thuc hien va theo doi
            Map<TopicPartition, Optional<NewPartitionReassignment>> request = new LinkedHashMap<>();
            for (Move m : moves) request.put(m.tp(), Optional.of(new NewPartitionReassignment(m.to())));
            long t0 = System.nanoTime();
            admin.alterPartitionReassignments(request).all().get();
            Set<TopicPartition> ours = new TreeSet<>(Comparator.comparing(TopicPartition::toString));
            ours.addAll(request.keySet());
            long prevBytes = 0;
            double prevT = 0;
            while (true) {
                Thread.sleep(1_000);
                int inFlight = admin.listPartitionReassignments(ours).reassignments().get().size();
                Map<Integer, Map<TopicPartition, Long>> now = sizes(admin, keep);
                long copied = moves.stream()
                        .mapToLong(m -> now.getOrDefault(m.adding(), Map.of()).getOrDefault(m.tp(), 0L)).sum();
                double t = (System.nanoTime() - t0) / 1e9;
                System.out.printf("  %5.1f s: con %2d partition dang chuyen, da chep %6.1f MB  (%5.1f MB/s)%n",
                        t, inFlight, copied / 1e6, (copied - prevBytes) / 1e6 / (t - prevT));
                prevBytes = copied;
                prevT = t;
                if (inFlight == 0) break;
            }
            System.out.printf("chuyen xong %d partition, %.1f MB trong %.1f s%n", moves.size(), prevBytes / 1e6, prevT);

            // 5. go throttle (kafka-reassign-partitions.sh --verify lam buoc nay khi thay da xong)
            Map<ConfigResource, Collection<AlterConfigOp>> clear = new LinkedHashMap<>();
            throttle.forEach((res, ops) -> clear.put(res, ops.stream()
                    .map(op -> new AlterConfigOp(new ConfigEntry(op.configEntry().name(), null), AlterConfigOp.OpType.DELETE))
                    .toList()));
            admin.incrementalAlterConfigs(clear).all().get();
            System.out.println("da go throttle tren " + leaderSide.size() + " topic va " + live.size() + " broker");

            Map<Integer, Map<TopicPartition, Long>> after = sizes(admin, live);
            System.out.printf("broker %d con %d replica%s%n", victim, after.getOrDefault(victim, Map.of()).size(),
                    after.getOrDefault(victim, Map.of()).isEmpty() ? ": tat may an toan" : ": CHUA tat duoc");
        }
    }

    static AlterConfigOp set(String name, String value) {
        return new AlterConfigOp(new ConfigEntry(name, value), AlterConfigOp.OpType.SET);
    }

    /** broker -> (partition -> so byte tren dia), doc bang DescribeLogDirs (giong kafka-log-dirs.sh). */
    static Map<Integer, Map<TopicPartition, Long>> sizes(Admin admin, Collection<Integer> brokers) throws Exception {
        Map<Integer, Map<TopicPartition, Long>> res = new HashMap<>();
        for (Map.Entry<Integer, Map<String, LogDirDescription>> e : admin.describeLogDirs(brokers).allDescriptions().get().entrySet()) {
            Map<TopicPartition, Long> m = new HashMap<>();
            e.getValue().values().forEach(d -> d.replicaInfos().forEach((tp, info) -> m.put(tp, info.size())));
            res.put(e.getKey(), m);
        }
        return res;
    }
}
