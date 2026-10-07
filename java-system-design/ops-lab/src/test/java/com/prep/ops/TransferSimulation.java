package com.prep.ops;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * Mo hinh MO: request den theo toc do co dinh, khong cho request truoc xong. Mo hinh dong (N user lap
 * lai) se tu cham lai khi server cham -> che mat duoi dai (coordinated omission).
 *
 * <pre>mvn -q gatling:test -Dpath=/transfers/bad -Drps=250 -Dseconds=30</pre>
 */
public class TransferSimulation extends Simulation {

    private final String path = System.getProperty("path", "/transfers/good");
    private final int rps = Integer.getInteger("rps", 250);
    private final int seconds = Integer.getInteger("seconds", 30);

    private final Iterator<Map<String, Object>> accounts = Stream.generate(() -> {
        var r = ThreadLocalRandom.current();
        long from = r.nextLong(1, 1001);
        long to = from % 1000 + 1;
        return Map.<String, Object>of("from", from, "to", to);
    }).iterator();

    private final HttpProtocolBuilder protocol = http.baseUrl("http://localhost:8080");

    private final ScenarioBuilder scn = scenario(path + " @ " + rps + " rps")
            .feed(accounts)
            .exec(http("transfer").post(path + "?from=#{from}&to=#{to}&amount=100").check(status().is(200)));

    {
        setUp(scn.injectOpen(constantUsersPerSec(rps).during(seconds))).protocols(protocol);
    }
}
