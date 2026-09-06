package com.prep.cleancode.after;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Thay the hoan toan cai switch + 14 field {@code @Qualifier} o ban before.
 *
 * <p>Single Responsibility: class nay chi lam MOT viec - tra cuu service -> HealthCheck. Khong
 * goi check, khong tong hop ket qua, khong format response. So voi ban before dang lam ca 4 viec.
 *
 * <p>Trong Spring chi can:
 * <pre>{@code
 * @Component
 * class HealthCheckRegistry {
 *     HealthCheckRegistry(List<HealthCheck> checks) { ... }  // Spring tu inject moi bean
 * }
 * }</pre>
 * Khong con {@code @Autowired} field, khong con {@code @Qualifier}, khong con switch.
 */
public final class HealthCheckRegistry {

    private final Map<ServiceName, HealthCheck> byService;

    /**
     * FAIL FAST o luc khoi tao, khong phai luc chay.
     *
     * <p>Ban before phat hien thieu service o {@code default:} cua switch -> loi xuat hien luc
     * request dau tien cham vao service do, co the la 3 gio sang. O day, neu cau hinh sai
     * (2 bean cung nhan mot service) thi ung dung KHONG KHOI DONG DUOC. Loi luc deploy re hon
     * loi luc 3 gio sang rat nhieu lan.
     *
     * <p>Chu y: KHONG dung {@code Collectors.toMap(HealthCheck::service, c -> c)} o day. No cung
     * nem khi trung key, nhung message cua no ({@code IllegalStateException: Duplicate key ...})
     * khong noi ro class nao dung nhau -> kho debug. Vong lap tuong minh cho message tot hon.
     */
    public HealthCheckRegistry(List<HealthCheck> checks) {
        if (checks == null || checks.isEmpty()) {
            throw new IllegalArgumentException("At least one HealthCheck must be registered");
        }
        // EnumMap: nhanh hon HashMap voi key la enum, va giu thu tu khai bao cua enum.
        var map = new EnumMap<ServiceName, HealthCheck>(ServiceName.class);
        for (HealthCheck check : checks) {
            ServiceName service = check.service();
            if (service == null) {
                throw new IllegalArgumentException(
                        check.getClass().getName() + " returned null from service()");
            }
            HealthCheck existing = map.put(service, check);
            if (existing != null) {
                throw new IllegalStateException(
                        "Two HealthCheck beans claim service %s: %s and %s"
                                .formatted(service, existing.getClass().getName(), check.getClass().getName()));
            }
        }
        this.byService = Map.copyOf(map);
    }

    /**
     * Tra ve Optional thay vi nem exception. Ly do: "service nay chua co health check" la trang
     * thai BINH THUONG (vi du dang rollout dan), khong phai loi lap trinh. Con
     * {@code UnsupportedOperationException} o ban before bien no thanh su co.
     */
    public Optional<HealthCheck> find(ServiceName service) {
        return Optional.ofNullable(byService.get(service));
    }

    /** Cac service THUC SU co health check - dung cai nay thay vi hardcode List.of(...). */
    public Set<ServiceName> registeredServices() {
        return byService.keySet();
    }

    /** Snapshot khong doi duoc - caller khong the sua trang thai ben trong registry. */
    public List<HealthCheck> all() {
        return List.copyOf(new ArrayList<>(byService.values()));
    }
}
