package com.prep.dist;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Saga orchestration toi gian: moi buoc co hanh dong + hanh dong bu. Buoc n loi -> chay bu n-1..1
 * theo thu tu NGUOC. Trang thai saga phai luu DB (o day giu trong bo nho cho gon).
 */
public final class Saga {
    public interface Step { String name(); void run(); void compensate(); }

    public enum Status { COMPLETED, COMPENSATED }

    public record Outcome(Status status, List<String> log) {}

    public static Outcome execute(List<Step> steps) {
        List<String> log = new ArrayList<>();
        Deque<Step> done = new ArrayDeque<>();
        for (Step s : steps) {
            try {
                s.run(); done.push(s); log.add("OK   " + s.name());
            } catch (RuntimeException e) {
                log.add("FAIL " + s.name() + ": " + e.getMessage());
                while (!done.isEmpty()) { Step c = done.pop(); c.compensate(); log.add("UNDO " + c.name()); }
                return new Outcome(Status.COMPENSATED, log);
            }
        }
        return new Outcome(Status.COMPLETED, log);
    }

    static Step step(String name, Runnable run, Runnable undo) {
        return new Step() {
            public String name() { return name; }
            public void run() { run.run(); }
            public void compensate() { undo.run(); }
        };
    }

    public static void main(String[] a) {
        long[] wallet = {1_000};      // vi khach
        long[] merchant = {0};
        boolean[] fail = {false};
        List<Step> steps = List.of(
                step("reserve 300 tu vi", () -> wallet[0] -= 300, () -> wallet[0] += 300),
                step("fraud check", () -> {}, () -> {}),
                step("credit merchant 300", () -> { if (fail[0]) throw new IllegalStateException("merchant bank timeout"); merchant[0] += 300; },
                        () -> merchant[0] -= 300),
                step("gui thong bao", () -> {}, () -> {}));
        var ok = execute(steps);
        System.out.println(ok.status() + " " + ok.log() + " -> vi=" + wallet[0] + " merchant=" + merchant[0]);
        wallet[0] = 1_000; merchant[0] = 0; fail[0] = true;
        var bad = execute(steps);
        System.out.println(bad.status() + " " + bad.log() + " -> vi=" + wallet[0] + " merchant=" + merchant[0]);
    }
}
