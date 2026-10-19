package com.prep.onbai.pool;

import java.util.concurrent.CyclicBarrier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Moi request: transaction ngoai (giu connection #1) goi mot buoc REQUIRES_NEW (can connection #2).
 * N request cung luc tren pool N connection: ai cung giu #1 va cho #2 -> khong ai xong -> het connectionTimeout.
 */
public final class NestedTransactions {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate outer;
    private final TransactionTemplate requiresNew;

    public NestedTransactions(DataSource ds) {
        DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
        this.jdbc = new JdbcTemplate(ds);
        this.outer = new TransactionTemplate(tm);
        this.requiresNew = new TransactionTemplate(tm);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** allHoldFirst: barrier de moi request giu connection #1 truoc khi xin #2 (dung lai luc tai cao). */
    public void handleRequest(CyclicBarrier allHoldFirst) {
        outer.executeWithoutResult(s -> {
            jdbc.queryForObject("SELECT 1", Integer.class);              // connection #1 dang bi giu
            await(allHoldFirst);
            requiresNew.executeWithoutResult(s2 ->                         // xin connection #2
                    jdbc.queryForObject("SELECT 2", Integer.class));
        });
    }

    private static void await(CyclicBarrier b) {
        try {
            b.await();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
