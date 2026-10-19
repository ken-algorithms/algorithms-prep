package com.prep.onbai.pgconcurrency.jpa;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletRepository wallets;
    private final AuditRepository audits;
    private final AuditService auditService;

    public WalletService(WalletRepository wallets, AuditRepository audits, AuditService auditService) {
        this.wallets = wallets;
        this.audits = audits;
        this.auditService = auditService;
    }

    /** Optimistic: doc khong khoa; luc commit Hibernate ghi "UPDATE ... WHERE id=? AND version=?". */
    @Transactional
    public void withdrawOptimistic(long id, long amount) {
        Wallet w = wallets.findById(id).orElseThrow();
        if (w.getBalance() < amount) {
            throw new IllegalStateException("khong du tien");
        }
        w.setBalance(w.getBalance() - amount); // dirty checking, chua co SQL nao o day
    }

    /** Pessimistic: SELECT ... FOR UPDATE ngay tu dau; ai den sau phai cho. */
    @Transactional
    public void withdrawPessimistic(long id, long amount) {
        Wallet w = wallets.lockById(id).orElseThrow();
        if (w.getBalance() < amount) {
            throw new IllegalStateException("khong du tien");
        }
        w.setBalance(w.getBalance() - amount);
    }

    /** Atomic: mot cau UPDATE co dieu kien, rowcount noi len ket qua. */
    @Transactional
    public boolean withdrawAtomic(long id, long amount) {
        return wallets.debitIfEnough(id, amount) == 1;
    }

    /** BAY: this.auditInNewTx() khong di qua proxy -> REQUIRES_NEW bi bo qua -> audit rollback cung. */
    @Transactional
    public void withdrawWithSelfInvokedAudit(long id, long amount) {
        auditInNewTx("thu rut " + amount + " tu vi " + id);
        failIfNotEnough(id, amount);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditInNewTx(String message) {
        audits.save(new AuditLog(message));
    }

    /** SUA: audit o bean KHAC -> goi qua proxy -> REQUIRES_NEW that su mo transaction rieng. */
    @Transactional
    public void withdrawWithAudit(long id, long amount) {
        auditService.record("thu rut " + amount + " tu vi " + id);
        failIfNotEnough(id, amount);
    }

    private void failIfNotEnough(long id, long amount) {
        if (!withdrawAtomic(id, amount)) { // goi noi bo: van chay trong transaction hien tai, khong sao
            throw new IllegalStateException("khong du tien");
        }
    }
}
