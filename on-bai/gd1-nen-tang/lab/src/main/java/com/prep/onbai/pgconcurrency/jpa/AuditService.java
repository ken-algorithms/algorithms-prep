package com.prep.onbai.pgconcurrency.jpa;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Audit phai con lai ke ca khi nghiep vu that bai -> transaction rieng, o bean rieng. */
@Service
public class AuditService {

    private final AuditRepository audits;

    public AuditService(AuditRepository audits) {
        this.audits = audits;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String message) {
        audits.save(new AuditLog(message));
    }
}
