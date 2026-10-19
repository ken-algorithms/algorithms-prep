package com.prep.onbai.ledger;

import com.prep.onbai.ledger.BalanceQueries.Balance;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Read-your-writes khi doc tu replica: client gui kem seq nhan duoc luc ghi (minSeq).
 * Replica chua bat kip seq do -> doc primary. So du khong bao gio "lui" trong mat mot khach.
 */
public final class ReadYourWrites {

    public record Read(Balance balance, String servedBy) {}

    private final JdbcTemplate primary;
    private final JdbcTemplate replica;

    public ReadYourWrites(DataSource primary, DataSource replica) {
        this.primary = new JdbcTemplate(primary);
        this.replica = new JdbcTemplate(replica);
    }

    public Read balance(long accountId, long minSeq) {
        Balance fromReplica = BalanceQueries.current(replica, accountId);
        if (fromReplica.asOfSeq() >= minSeq) {
            return new Read(fromReplica, "replica");
        }
        return new Read(BalanceQueries.current(primary, accountId), "primary");
    }
}
