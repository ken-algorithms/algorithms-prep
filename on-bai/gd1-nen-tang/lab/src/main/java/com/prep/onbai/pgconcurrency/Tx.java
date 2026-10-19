package com.prep.onbai.pgconcurrency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;

/**
 * Mot transaction JDBC tran, de viet interleaving thanh dong thoi gian doc duoc:
 * <pre>
 *   Tx t1 = Tx.begin(ds, READ_COMMITTED), t2 = Tx.begin(ds, READ_COMMITTED);
 *   int a = t1.queryInt("SELECT balance ...");      // T1 doc
 *   int b = t2.queryInt("SELECT balance ...");      // T2 doc
 *   t1.update(...); t1.commit();                    // T1 ghi
 *   Future&lt;Integer&gt; f = t2.async(() -&gt; t2.update(...));   // T2 ghi, co the bi CHAN
 * </pre>
 * Moi Tx co mot luong rieng: lenh async chay tren luong do, nen lenh bi chan khoa khong lam treo test.
 */
public final class Tx implements AutoCloseable {

    public static final int READ_COMMITTED = Connection.TRANSACTION_READ_COMMITTED;
    public static final int REPEATABLE_READ = Connection.TRANSACTION_REPEATABLE_READ;
    public static final int SERIALIZABLE = Connection.TRANSACTION_SERIALIZABLE;

    private final Connection c;
    private final ExecutorService thread = Executors.newSingleThreadExecutor();

    private Tx(Connection c) {
        this.c = c;
    }

    public static Tx begin(DataSource ds, int isolation) {
        try {
            Connection c = ds.getConnection();
            c.setAutoCommit(false);
            c.setTransactionIsolation(isolation);
            return new Tx(c);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public int queryInt(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = prepare(sql, args); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : Integer.MIN_VALUE; // MIN_VALUE = khong co dong nao
        }
    }

    /** Mot dong, nhieu cot int (vd balance, version). */
    public int[] queryRow(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = prepare(sql, args); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return new int[0];
            }
            int[] row = new int[rs.getMetaData().getColumnCount()];
            for (int i = 0; i < row.length; i++) {
                row[i] = rs.getInt(i + 1);
            }
            return row;
        }
    }

    public int update(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = prepare(sql, args)) {
            return ps.executeUpdate();
        }
    }

    public void execute(String sql) throws SQLException {
        try (var s = c.createStatement()) {
            s.execute(sql);
        }
    }

    /** Chay lenh tren luong rieng cua transaction nay: tra Future de test kiem tra no co dang bi chan khong. */
    public <T> Future<T> async(Callable<T> work) {
        return thread.submit(work);
    }

    public void commit() throws SQLException {
        c.commit();
    }

    public void rollback() {
        try {
            c.rollback();
        } catch (SQLException ignored) {
            // connection hong thi dong luon o close()
        }
    }

    @Override
    public void close() {
        rollback();
        thread.shutdownNow();
        try {
            c.close();
        } catch (SQLException ignored) {
            // da dong
        }
    }

    private PreparedStatement prepare(String sql, Object... args) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
        return ps;
    }
}
