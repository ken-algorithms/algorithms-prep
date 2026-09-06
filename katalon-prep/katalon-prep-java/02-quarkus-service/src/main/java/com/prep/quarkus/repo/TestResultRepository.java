package com.prep.quarkus.repo;

import com.prep.quarkus.domain.ResultStatus;
import com.prep.quarkus.domain.TestResultRow;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TestResultRepository implements PanacheRepository<TestResultRow> {

    /**
     * BULK INSERT co batch + flush/clear dinh ky.
     *
     * <p>Day la diem phong van hay hoi: "insert 10.000 dong the nao cho nhanh?".
     *
     * <p>Ba thu phai lam:
     * <ol>
     *   <li>{@code hibernate.jdbc.batch_size} trong application.properties -> JDBC gui theo lo.
     *   <li>Dung SEQUENCE (khong dung IDENTITY) cho id. IDENTITY BUOC Hibernate phai insert tung
     *       dong de lay id sinh ra -> BATCH BI VO HOAN TOAN. Day la bay rat pho bien.
     *   <li>{@code flush()} + {@code clear()} moi batch -> khong de persistence context phinh
     *       vo han (10.000 entity trong memory = OutOfMemory o quy mo that).
     * </ol>
     */
    public void persistBatch(List<TestResultRow> rows, int batchSize, EntityManager em) {
        for (int i = 0; i < rows.size(); i++) {
            persist(rows.get(i));
            if ((i + 1) % batchSize == 0) {
                em.flush();
                em.clear();
            }
        }
        em.flush();
        em.clear();
    }

    /**
     * Dem theo status bang MOT query group-by, thay vi 4 query count.
     *
     * <p>Neu goi count() cho tung status: 4 lan quet index. Group-by: 1 lan.
     * O bang hang ty dong thi khac biet nay la thay doi ve bac, khong phai vi chinh.
     */
    public Map<ResultStatus, Long> countByStatus(Long runId) {
        List<Object[]> rows = getEntityManager()
                .createQuery(
                        "select r.status, count(r) from TestResultRow r where r.runId = :runId group by r.status",
                        Object[].class)
                .setParameter("runId", runId)
                .getResultList();

        var counts = new EnumMap<ResultStatus, Long>(ResultStatus.class);
        for (Object[] row : rows) {
            counts.put((ResultStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    /** p95 duration - tinh trong DB, khong keo het du lieu ve app. */
    public Integer percentileDuration(Long runId, double percentile) {
        Object value = getEntityManager()
                .createNativeQuery(
                        """
                        SELECT percentile_disc(:p) WITHIN GROUP (ORDER BY duration_ms)
                        FROM test_result WHERE run_id = :runId
                        """)
                .setParameter("p", percentile)
                .setParameter("runId", runId)
                .getSingleResult();
        return value == null ? null : ((Number) value).intValue();
    }

    public long deleteByRunId(Long runId) {
        return delete("runId", runId);
    }
}
