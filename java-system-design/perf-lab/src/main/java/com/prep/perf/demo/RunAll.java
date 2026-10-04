package com.prep.perf.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Chay toan bo demo nhom 2-4 theo thu tu. D18 chay trong JVM con voi -Xmx128m de thay OOM that.
 *
 * <pre>java -cp target/benchmarks.jar com.prep.perf.demo.RunAll</pre>
 */
public final class RunAll {

    private RunAll() {}

    public static void main(String[] args) throws Exception {
        section("P09 remote call inside @Transactional -> connection pool exhaustion");
        D09PoolExhaustion.main(args);
        section("P10 no timeout on downstream -> thread starvation spreads to other endpoints");
        D10NoTimeout.main(args);
        section("P12 virtual thread pinning (synchronized + blocking IO)");
        D12Pinning.main(args);
        section("P13 unbounded queue under overload");
        D13UnboundedQueue.main(args);
        section("P15 N+1 queries");
        D15NPlusOne.main(args);
        section("P18 unbounded cache (child JVM, -Xmx128m)");
        child("-Xmx128m", D18UnboundedCache.class.getName(), "bad");
        child("-Xmx128m", D18UnboundedCache.class.getName(), "good");
        section("P19 materialize everything vs stream (child JVM, -Xmx512m)");
        child("-Xmx512m", D19MaterializeAll.class.getName(), "");
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static void child(String xmx, String mainClass, String arg) throws Exception {
        String java = ProcessHandle.current().info().command().orElse("java");
        var cmd = new java.util.ArrayList<>(List.of(java, xmx, "-cp", System.getProperty("java.class.path"), mainClass));
        if (!arg.isEmpty()) {
            cmd.add(arg);
        }
        var p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        try (var r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            r.lines().filter(l -> !l.startsWith("Picked up JAVA_TOOL_OPTIONS")).forEach(System.out::println);
        }
        p.waitFor();
    }
}
