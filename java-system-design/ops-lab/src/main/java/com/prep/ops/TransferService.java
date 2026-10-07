package com.prep.ops;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * P09 tren stack that. Hai cach viet CUNG mot nghiep vu chuyen tien co buoc goi doi tac (fraud check).
 */
@Service
public class TransferService {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final RestClient partner;

    public TransferService(JdbcTemplate jdbc, TransactionTemplate tx, @Value("${partner.base-url}") String partnerUrl) {
        this.jdbc = jdbc;
        this.tx = tx;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(300))
                .build());
        factory.setReadTimeout(Duration.ofSeconds(2));           // P10: luon co timeout
        this.partner = RestClient.builder().baseUrl(partnerUrl).requestFactory(factory).build();
    }

    /** XAU: khoa dong + giu connection trong suot thoi gian cho doi tac. */
    @Transactional
    public String transferBad(long from, long to, long amount) {
        Long balance = jdbc.queryForObject("select balance from account where id = ? for update", Long.class, from);
        String verdict = callPartner();                          // 50 ms, van giu connection + row lock
        if (balance == null || balance < amount) {
            throw new IllegalStateException("insufficient funds");
        }
        jdbc.update("update account set balance = balance - ? where id = ?", amount, from);
        jdbc.update("update account set balance = balance + ? where id = ?", amount, to);
        jdbc.update("insert into ledger_entry(account_id, amount) values (?, ?), (?, ?)", from, -amount, to, amount);
        return verdict;
    }

    /** SUA: goi doi tac truoc, transaction chi bao phan DB (vai ms). */
    public String transferGood(long from, long to, long amount) {
        String verdict = callPartner();                          // khong giu connection nao
        return tx.execute(s -> {
            int debited = jdbc.update("update account set balance = balance - ? where id = ? and balance >= ?",
                    amount, from, amount);
            if (debited == 0) {
                throw new IllegalStateException("insufficient funds");
            }
            jdbc.update("update account set balance = balance + ? where id = ?", amount, to);
            jdbc.update("insert into ledger_entry(account_id, amount) values (?, ?), (?, ?)", from, -amount, to, amount);
            return verdict;
        });
    }

    private String callPartner() {
        return partner.get().uri("/check").retrieve().body(String.class);
    }
}
