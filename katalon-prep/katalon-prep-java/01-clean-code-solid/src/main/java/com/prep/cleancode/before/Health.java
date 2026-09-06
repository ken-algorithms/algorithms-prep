package com.prep.cleancode.before;

import java.util.HashMap;
import java.util.Map;

/**
 * Class mutable, getter/setter, details la HashMap tuy y (Map<String, Object>).
 * Hau qua: khong ai biet trong details co gi -> phai cast, phai null-check, khong type-safe.
 */
public class Health {

    private String status;
    private Map<String, Object> details = new HashMap<>();

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
