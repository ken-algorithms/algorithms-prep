package com.prep.cleancode.after;

/**
 * Thay cho String "status" o ban before.
 *
 * <p>Vi sao can UNKNOWN rieng, khong gop vao DOWN? Vi hai cai nay dan tin hieu khac nhau cho
 * nguoi on-call: DOWN = da goi va service tra loi that bai (co hanh dong ro rang). UNKNOWN = ta
 * KHONG BIET (timeout, mat mang) - co the service van song. Gop chung lai se gay bao dong gia
 * va lam sai SLO. Day la vi du cu the cua "modeling failure explicitly".
 */
public enum HealthStatus {
    UP,
    DOWN,
    UNKNOWN;

    /** Trang thai tong hop: xau nhat thang. UP < UNKNOWN < DOWN theo do nghiem trong. */
    public HealthStatus worseOf(HealthStatus other) {
        if (this == DOWN || other == DOWN) {
            return DOWN;
        }
        if (this == UNKNOWN || other == UNKNOWN) {
            return UNKNOWN;
        }
        return UP;
    }
}
