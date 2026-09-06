package com.prep.spring.repo;

import com.prep.spring.domain.RunStatus;
import com.prep.spring.domain.TestRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * BAY DA GAP THAT khi chay test lan dau:
 *
 * <p>Ban dau toi gom ca hai repository lam NESTED interface trong mot class {@code Repositories}
 * cho gon file. Ket qua: {@code No qualifying bean of type 'Repositories$TestRunRepository'} —
 * Spring Data KHONG tao proxy cho repository dat lam nested interface trong mot class bao ngoai.
 *
 * <p>Bai hoc: gom kieu vao mot "holder class" de tiet kiem file la di nguoc quy uoc cua framework.
 * Repository phai la interface TOP-LEVEL, moi cai mot file. Loi nay chi lo ra khi CHAY, khong lo
 * ra khi compile — them mot vi du cho quy tac "luon chay code, dung chi doc".
 */
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    // Derived query: Spring Data sinh implementation tu TEN METHOD.
    // Khac Panache (viet query string). Danh doi: ten dai va de sai chinh ta, nhung duoc kiem tra
    // luc khoi dong -> sai la ung dung khong start (fail fast), khong phai loi luc chay.
    Optional<TestRun> findByIdAndTenantId(Long id, String tenantId);

    List<TestRun> findByTenantIdOrderByStartedAtDesc(String tenantId, Pageable pageable);

    List<TestRun> findByTenantIdAndStatusOrderByStartedAtDesc(
            String tenantId, RunStatus status, Pageable pageable);
}
