package com.prep.cleancode.after;

/** Giong ban before - enum nay khong co van de gi, giu nguyen. */
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

    public String id() {
        return id;
    }
}
