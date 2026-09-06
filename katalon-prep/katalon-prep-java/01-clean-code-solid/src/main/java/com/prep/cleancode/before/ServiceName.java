package com.prep.cleancode.before;

/** Danh sach service downstream can health check. */
public enum ServiceName {
    AUTHENTICATION("auth"),
    INCIDENT_HUB("incidentHub"),
    PAYMENT_BRIDGE("paymentBridge"),
    ERP_BRIDGE("erpBridge"),
    ACCOUNT("account"),
    BATCH("batch"),
    CORE("core"),
    REPORTING("reporting");

    private final String id;

    ServiceName(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
