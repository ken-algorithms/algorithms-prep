package com.prep.cs;

import java.net.InetSocketAddress;
import java.net.Socket;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assumptions;

import com.prep.cs.counting.CountStore;

/** Dia chi ha tang cua lab (docker/cluster.yml). Test can Kafka/Postgres se BO QUA kem huong dan neu chua chay. */
public final class Infra {

    public static final String KAFKA = "localhost:19092,localhost:29092,localhost:39092";
    public static final String PG_URL = "jdbc:postgresql://localhost:55432/lab";
    private static final String HOW = "chua chay ha tang: docker compose -f docker/cluster.yml up -d --wait";

    private Infra() {}

    public static void requireKafka() {
        Assumptions.assumeTrue(open("localhost", 19092), "Kafka " + HOW);
    }

    public static DataSource requirePostgres() {
        Assumptions.assumeTrue(open("localhost", 55432), "Postgres " + HOW);
        return CountStore.dataSource(PG_URL, "lab", "lab");
    }

    private static boolean open(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
