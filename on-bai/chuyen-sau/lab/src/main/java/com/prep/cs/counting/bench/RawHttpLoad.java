package com.prep.cs.counting.bench;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import com.prep.cs.counting.LoadGen;

/**
 * Ban tai HTTP/1.1 toi gian bang socket tho: moi ket noi keep-alive, TCP_NODELAY, ghi ca header + body trong
 * MOT lan write (tranh bay Nagle + delayed ACK 40 ms khi header va body di hai goi). Do CPU cua TIEN TRINH
 * server tren moi request qua /metrics. Moi ket noi sinh san 64 request roi gui vong lai: client khong ton CPU
 * sinh JSON (may lab chi co 4 vCPU dung chung voi server va ca cum Kafka).
 *
 *   scripts/run.sh com.prep.cs.counting.bench.RawHttpLoad localhost 8080 64 15
 */
public final class RawHttpLoad {

    public static void main(String[] a) throws Exception {
        String host = a[0];
        int port = Integer.parseInt(a[1]), conns = Integer.parseInt(a[2]), seconds = Integer.parseInt(a[3]);
        List<String> keys = IntStream.range(0, 1_000_000).mapToObj(k -> "feature_" + k).toList();
        LoadGen gen = new LoadGen(keys);
        AtomicLong ok = new AtomicLong(), bad = new AtomicLong();
        long[] m0 = metrics(host, port);
        long t0 = System.nanoTime(), until = System.currentTimeMillis() + seconds * 1_000L;
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int c = 0; c < conns; c++) {
                long clientId = 800_000 + c;
                pool.submit(() -> {
                    SplittableRandom r = new SplittableRandom(clientId);
                    byte[][] reqs = new byte[64][];
                    for (int k = 0; k < reqs.length; k++) {
                        byte[] body = gen.body(clientId, 1, k, System.currentTimeMillis(), 20, r, new long[4]);
                        byte[] head = ("POST /events HTTP/1.1\r\nHost: " + host + "\r\nContent-Type: application/json\r\n"
                                + "Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
                        reqs[k] = new byte[head.length + body.length];
                        System.arraycopy(head, 0, reqs[k], 0, head.length);
                        System.arraycopy(body, 0, reqs[k], head.length, body.length);
                    }
                    try (Socket s = new Socket(host, port)) {
                        s.setTcpNoDelay(true);
                        OutputStream out = s.getOutputStream();
                        InputStream in = new BufferedInputStream(s.getInputStream(), 16 * 1024);
                        for (int seq = 0; System.currentTimeMillis() < until; seq++) {
                            out.write(reqs[seq & 63]);
                            out.flush();
                            int code = readResponse(in);
                            (code == 202 ? ok : bad).incrementAndGet();
                        }
                    }
                    return null;
                });
            }
        }
        double sec = (System.nanoTime() - t0) / 1e9;
        long[] m1 = metrics(host, port);
        long reqs = m1[0] - m0[0];
        System.out.printf("%d ket noi, %.1f s: %,d x 202, %d loi -> %,.0f request/s; CPU server %.1f us/request%s%n",
                conns, sec, ok.get(), bad.get(), ok.get() / sec, (m1[1] - m0[1]) / 1e3 / Math.max(1, reqs),
                m1[2] < 0 ? "" : String.format(" (trong do kernel %.1f us)", (m1[2] - m0[2]) / 1e3 / Math.max(1, reqs)));
    }

    /** Doc dong trang thai + header toi dong trong; bo qua body theo Content-Length. Tra ma trang thai. */
    static int readResponse(InputStream in) throws Exception {
        StringBuilder line = new StringBuilder();
        int code = -1, length = 0;
        while (true) {
            int b = in.read();
            if (b < 0) throw new IllegalStateException("server dong ket noi");
            if (b == '\n') {
                String l = line.toString().trim();
                line.setLength(0);
                if (l.isEmpty()) break;
                if (code < 0) code = Integer.parseInt(l.split(" ")[1]);
                else if (l.regionMatches(true, 0, "content-length:", 0, 15)) length = Integer.parseInt(l.substring(15).trim());
            } else {
                line.append((char) b);
            }
        }
        in.skipNBytes(length);
        return code;
    }

    static long[] metrics(String host, int port) throws Exception {
        try (Socket s = new Socket(host, port)) {
            s.getOutputStream().write(("GET /metrics HTTP/1.1\r\nHost: " + host + "\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            String resp = new String(s.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
            long req = Long.parseLong(resp.replaceAll("(?s).*\"requests\":(\\d+).*", "$1"));
            long cpu = Long.parseLong(resp.replaceAll("(?s).*\"cpuNanos\":(\\d+).*", "$1"));
            long sys = resp.contains("sysNanos") ? Long.parseLong(resp.replaceAll("(?s).*\"sysNanos\":(-?\\d+).*", "$1")) : -1;
            return new long[]{req, cpu, sys};
        }
    }
}
