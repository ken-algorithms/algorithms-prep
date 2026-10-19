package com.prep.onbai.pgconcurrency.jpa;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /** Sinh "select ... for update": khoa dong toi het transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> lockById(@Param("id") Long id);

    /**
     * Check-and-set nguyen tu trong MOT cau UPDATE: khong co khoang ho doc -> ghi.
     * Bulk update KHONG qua @Version -> tu tang version de nguoi dang giu ban cu van bi bat.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Wallet w set w.balance = w.balance - :amount, w.version = w.version + 1 "
            + "where w.id = :id and w.balance >= :amount")
    int debitIfEnough(@Param("id") Long id, @Param("amount") long amount);
}
