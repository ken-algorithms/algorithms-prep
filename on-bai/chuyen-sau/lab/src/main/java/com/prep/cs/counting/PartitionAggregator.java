package com.prep.cs.counting;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.TreeMap;

/**
 * State cua MOT partition trong aggregator: khu trung theo client, cong don delta THEO GIAY cho toi lan commit,
 * va hai moc offset:
 *   committedNext : offset da ghi trong Postgres (cung transaction voi so dem)
 *   next          : offset ke tiep se doc (da xu ly het toi next - 1)
 *   replayUntil   : record co offset < moc nay CHI nap vao state khu trung, khong dem (da dem truoc khi restart)
 * Duong nong (moi request) chi co: 1 lan tra client + 1 lan tra o giay, khong cap phat. Delta phut/gio/ngay
 * duoc cong tu delta giay LUC COMMIT (vai tram o), khong phai luc nhan request.
 */
public final class PartitionAggregator {

    public final int partition;
    long committedNext, next, replayUntil;
    private final LongObjectMap<ClientWindow> clients = new LongObjectMap<>(1 << 12);
    private final LongObjectMap<long[]> perSecond = new LongObjectMap<>(1 << 10);
    private final Decoded r = new Decoded();
    public long counted, duplicates, stale, tooOld, replayed, commits;

    public PartitionAggregator(int partition, long committedNext) {
        this.partition = partition;
        this.committedNext = committedNext;
        this.next = committedNext;
        this.replayUntil = committedNext;
    }

    /** Mot record Kafka = mot block nhieu request. timestampMs: thoi diem block duoc tao, dung de don client cu. */
    public void onBlock(long offset, long timestampMs, ByteBuffer block) {
        int n = block.getInt();
        boolean replay = offset < replayUntil;
        for (int i = 0; i < n; i++) {
            RequestRecord.read(block, r);
            ClientWindow w = clients.get(r.clientId);
            ClientWindow.Verdict v;
            if (w == null) {
                clients.put(r.clientId, new ClientWindow(r.epoch, r.seq, timestampMs));
                v = ClientWindow.Verdict.NEW;
            } else {
                v = w.accept(r.epoch, r.seq, timestampMs);
            }
            if (replay) {
                replayed++;
                continue;
            }
            switch (v) {
                case NEW -> add(r);
                case DUPLICATE -> duplicates++;
                case STALE -> stale++;
                case TOO_OLD -> tooOld++;
            }
        }
        if (!replay) next = offset + 1;
    }

    private void add(Decoded d) {
        counted++;
        long sec = d.tsMillis / 1000;
        long[] c = perSecond.get(sec);
        if (c == null) {
            c = new long[4];
            perSecond.put(sec, c);
        }
        for (int i = 0; i < 4; i++) c[i] += d.counts[i];
    }

    /** Delta cua ca 4 tang, sap theo (tang, bucket): thu tu khoa dong co dinh cho moi transaction. */
    TreeMap<CountStore.Cell, long[]> deltas() {
        TreeMap<CountStore.Cell, long[]> out = new TreeMap<>();
        perSecond.forEach((sec, c) -> {
            for (Tier t : Tier.values()) {
                long[] acc = out.computeIfAbsent(new CountStore.Cell(t, t.bucketOf(sec)), k -> new long[4]);
                for (int i = 0; i < 4; i++) acc[i] += c[i];
            }
        });
        return out;
    }

    /** false = partition da sang tay aggregator khac (fencing) — bo state nay, khong duoc ghi tiep. */
    public boolean commit(CountStore store, Connection c, String topic) throws SQLException {
        if (next == committedNext && perSecond.size() == 0) return true;
        if (!store.commit(c, topic, partition, committedNext, next, deltas())) return false;
        committedNext = next;
        perSecond.clear();
        commits++;
        return true;
    }

    /** Bo client im lang lau hon cua so khu trung (tinh theo thoi gian cua record, khong theo dong ho may). */
    public int evictIdle(long olderThanMs) {
        return clients.removeIf(w -> w.lastSeenMillis < olderThanMs);
    }

    public int trackedClients() {
        return clients.size();
    }

    public int pendingSeconds() {
        return perSecond.size();
    }
}
