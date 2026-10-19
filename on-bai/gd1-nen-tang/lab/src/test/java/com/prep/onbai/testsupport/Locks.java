package com.prep.onbai.testsupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

/** Quan sat khoa trong Postgres de test interleaving KHONG phai doan bang sleep. */
public final class Locks {

    private Locks() {}

    /**
     * Doi toi khi co it nhat n session dang cho khoa trong database nay (pg_stat_activity, wait_event_type = 'Lock').
     * Hoi tren connection rieng, autocommit: trong mot transaction, pg_stat_activity bi chup mot lan roi giu nguyen.
     */
    public static void awaitWaiters(DataSource ds, int n) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() AND wait_event_type = 'Lock'")) {
            while (System.nanoTime() < deadline) {
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) >= n) {
                        return;
                    }
                }
                Thread.sleep(10);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        throw new AssertionError("sau 10 giay khong thay " + n + " session cho khoa");
    }
}
