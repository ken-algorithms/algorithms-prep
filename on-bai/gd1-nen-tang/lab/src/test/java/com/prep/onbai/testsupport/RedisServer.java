package com.prep.onbai.testsupport;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import redis.clients.jedis.JedisPooled;

/** redis-server THAT tren cong ngau nhien, lay tu PATH. Khong co redis-server thi test Redis tu SKIP. */
public final class RedisServer implements AutoCloseable {

    private final Process process;
    public final int port;

    private RedisServer(Process process, int port) {
        this.process = process;
        this.port = port;
    }

    public static RedisServer startOrSkip() {
        String bin = List.of("/usr/bin/redis-server", "/usr/local/bin/redis-server", "/opt/homebrew/bin/redis-server")
                .stream().filter(p -> new File(p).canExecute()).findFirst().orElse(null);
        Assumptions.assumeTrue(bin != null, "khong tim thay redis-server: bo qua test Redis");
        try {
            int port;
            try (ServerSocket s = new ServerSocket(0)) {
                port = s.getLocalPort();
            }
            Process p = new ProcessBuilder(bin, "--port", String.valueOf(port), "--bind", "127.0.0.1",
                    "--save", "", "--appendonly", "no").redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            RedisServer server = new RedisServer(p, port);
            long deadline = System.currentTimeMillis() + 5_000;
            while (System.currentTimeMillis() < deadline) {
                try (JedisPooled j = server.client()) {
                    if ("PONG".equals(j.ping())) {
                        return server;
                    }
                } catch (RuntimeException notYet) {
                    Thread.sleep(50);
                }
            }
            server.close();
            throw new IllegalStateException("redis-server khong len");
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public JedisPooled client() {
        return new JedisPooled("127.0.0.1", port);
    }

    @Override
    public void close() {
        process.destroy();
    }
}
