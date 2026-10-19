package com.prep.cs.kafka;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.Node;

/**
 * Buoc dau tien voi moi cum Kafka: noi vao bootstrap, hoi metadata, in ra DUNG dia chi ma broker quang ba.
 * Client se noi toi cac dia chi nay o buoc sau, khong phai toi bootstrap.
 *
 *   scripts/run.sh com.prep.cs.kafka.ConnectCheck localhost:19092
 */
public final class ConnectCheck {

    public static void main(String[] args) throws Exception {
        String bootstrap = args.length > 0 ? args[0] : "localhost:19092";
        Map<String, Object> conf = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5_000,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 10_000);
        long t0 = System.nanoTime();
        try (Admin admin = Admin.create(conf)) {
            DescribeClusterResult c = admin.describeCluster();
            String clusterId = c.clusterId().get(10, TimeUnit.SECONDS);
            Node controller = c.controller().get(10, TimeUnit.SECONDS);
            System.out.printf("bootstrap      = %s%n", bootstrap);
            System.out.printf("cluster id     = %s%n", clusterId);
            // Client chi thay broker, khong thay controller KRaft: truong nay la mot broker ngau nhien.
            System.out.printf("'controller'   = broker %d (client khong noi thang voi controller KRaft)%n", controller.id());
            for (Node n : c.nodes().get(10, TimeUnit.SECONDS)) {
                System.out.printf("broker %d       -> %s:%d  rack=%s%n", n.id(), n.host(), n.port(), n.rack());
            }
            System.out.printf("topics         = %s%n", admin.listTopics().names().get(10, TimeUnit.SECONDS));
            System.out.printf("thoi gian      = %d ms%n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0));
        }
    }
}
