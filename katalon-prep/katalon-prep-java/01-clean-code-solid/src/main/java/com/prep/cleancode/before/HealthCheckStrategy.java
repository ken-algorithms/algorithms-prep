package com.prep.cleancode.before;

import java.util.concurrent.CompletableFuture;

/**
 * Interface goc trong RCI. Van de: strategy KHONG tu khai bao no phuc vu service nao,
 * nen phia goi buoc phai co switch/@Qualifier de biet dung cai nao.
 * Do la ly do goc sinh ra loi thiet ke o ServiceHealthAggregatorBefore.
 */
public interface HealthCheckStrategy {
    CompletableFuture<Health> doCheck();
}
