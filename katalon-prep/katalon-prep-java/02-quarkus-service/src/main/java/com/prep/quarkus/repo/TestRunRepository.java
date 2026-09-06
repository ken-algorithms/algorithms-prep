package com.prep.quarkus.repo;

import com.prep.quarkus.domain.RunStatus;
import com.prep.quarkus.domain.TestRun;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/** Repository pattern: truy van tach khoi entity. */
@ApplicationScoped
public class TestRunRepository implements PanacheRepository<TestRun> {

    public Optional<TestRun> findByIdForTenant(Long id, String tenantId) {
        // Multi-tenant: MOI query phai loc theo tenant. Neu quen mot cho la ro ri du lieu
        // giua khach hang - loai bug nghiem trong nhat cua SaaS.
        return find("id = ?1 and tenantId = ?2", id, tenantId).firstResultOptional();
    }

    public List<TestRun> findPage(String tenantId, RunStatus status, int pageIndex, int pageSize) {
        // Panache: query rong ("") + Sort. Tranh string concat de khong mo duong cho SQL injection.
        if (status == null) {
            return find("tenantId = ?1", Sort.by("startedAt").descending(), tenantId)
                    .page(Page.of(pageIndex, pageSize))
                    .list();
        }
        return find(
                        "tenantId = ?1 and status = ?2",
                        Sort.by("startedAt").descending(),
                        tenantId,
                        status)
                .page(Page.of(pageIndex, pageSize))
                .list();
    }
}
