package com.prep.spring.repo;

import com.prep.spring.domain.ResultStatus;
import com.prep.spring.domain.TestResultRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestResultRepository extends JpaRepository<TestResultRow, Long> {

    /** Mot query group-by thay vi 4 query count - giong module 02. */
    @Query("select r.status, count(r) from TestResultRow r where r.runId = :runId group by r.status")
    List<Object[]> countByStatusRaw(@Param("runId") Long runId);

    @Query(
            value =
                    """
                    SELECT percentile_disc(:p) WITHIN GROUP (ORDER BY duration_ms)
                    FROM test_result WHERE run_id = :runId
                    """,
            nativeQuery = true)
    Integer percentileDuration(@Param("runId") Long runId, @Param("p") double percentile);

    long countByRunId(Long runId);

    long countByRunIdAndStatus(Long runId, ResultStatus status);
}
