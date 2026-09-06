package com.prep.cleancode.after;

import java.util.concurrent.CompletableFuture;

/**
 * ==========================================================================
 * THAY DOI QUAN TRONG NHAT CUA CA MODULE NAY: strategy TU KHAI BAO service().
 * ==========================================================================
 *
 * <p>Ban before: {@code interface HealthCheckStrategy { CompletableFuture<Health> doCheck(); }}
 * - strategy khong biet no phuc vu service nao, nen NGUOI GOI phai biet -> sinh ra switch + 14
 * field @Qualifier.
 *
 * <p>Ban after: strategy tu tra loi "toi lo service nao". Nho vay registry co the tu build map,
 * va them service moi KHONG can sua bat ky file nao dang co. Do chinh la Open/Closed.
 *
 * <p>Trong Spring, dieu nay cho phep viet:
 * {@code public HealthCheckRegistry(List<HealthCheck> checks)} - Spring tu inject MOI bean
 * implement HealthCheck. Them 1 @Component moi la xong, khong sua registry.
 *
 * <p>HOP DONG (contract) - moi implementation PHAI tuan thu, day la phan LSP:
 * <ol>
 *   <li>{@code check()} khong bao gio nem exception truc tiep; loi phai nam trong future.
 *   <li>khong bao gio hoan thanh voi gia tri null.
 *   <li>{@code service()} phai tra ve cung mot gia tri moi lan goi (stable, dung lam map key).
 * </ol>
 */
public interface HealthCheck {

    /** Service ma implementation nay chiu trach nhiem kiem tra. Phai on dinh. */
    ServiceName service();

    /** Khong nem, khong tra null. Loi duoc bieu dien bang Health.down/unknown hoac future failed. */
    CompletableFuture<Health> check();
}
