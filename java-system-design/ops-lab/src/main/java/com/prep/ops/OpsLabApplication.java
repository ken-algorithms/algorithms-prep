package com.prep.ops;

import com.sun.net.httpserver.HttpServer;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Executors;

/**
 * App tham khao cho lab tuan 12. Khoi dong 3 thu:
 * <ol>
 *   <li>PostgreSQL 16 that (embedded, khong Docker) - khong chay duoi user root;</li>
 *   <li>"doi tac" HTTP gia o cong 18081, tra loi sau {@code partner.delay-ms} (mac dinh 50 ms);</li>
 *   <li>Spring Boot o cong 8080 voi HikariCP pool 10.</li>
 * </ol>
 * {@code java -jar ops-lab.jar retry-storm} chay demo retry storm, khong khoi dong gi ca.
 */
@SpringBootApplication
public class OpsLabApplication {

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("retry-storm")) {
            RetryStorm.main(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                pg.close();
            } catch (Exception ignored) {
                // JVM dang tat
            }
        }));
        System.setProperty("spring.datasource.url", pg.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");

        long delayMs = Long.parseLong(System.getProperty("partner.delay-ms", "50"));
        startPartnerStub(18081, delayMs);
        SpringApplication.run(OpsLabApplication.class, args);
    }

    /** Doi tac gia: moi request mot virtual thread, nen ban than doi tac khong bao gio la nut that. */
    static void startPartnerStub(int port, long delayMs) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 1024);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/check", ex -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = "APPROVED".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
    }
}
