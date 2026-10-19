package com.prep.cs.counting;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Sinh body request giong client that: ~20 item, phan lon key hop le, mot phan key gia, mot it item hong.
 * Dong thoi tinh SAN ket qua dung (oracle) theo luat phan loai, doc lap voi RequestDecoder, de test doi chieu.
 */
public final class LoadGen {

    private final List<String> validKeys;

    public LoadGen(List<String> validKeys) {
        this.validKeys = validKeys;
    }

    /** expected[4] duoc CONG them so item moi outcome cua request nay. */
    public byte[] body(long clientId, int epoch, long seq, long tsMillis, int items, SplittableRandom r,
                       long[] expected) {
        StringBuilder sb = new StringBuilder(64 + items * 40);
        sb.append("{\"clientId\":").append(clientId).append(",\"epoch\":").append(epoch)
                .append(",\"seq\":").append(seq).append(",\"ts\":").append(tsMillis).append(",\"items\":[");
        for (int i = 0; i < items; i++) {
            if (i > 0) sb.append(',');
            int roll = r.nextInt(100);
            if (roll < 75) {                                  // key that, value boolean
                boolean v = r.nextInt(100) < 60;
                sb.append("{\"key\":\"").append(validKeys.get(r.nextInt(validKeys.size())))
                        .append("\",\"value\":").append(v).append('}');
                expected[v ? 0 : 1]++;
            } else if (roll < 90) {                           // key gia: FAKE_KEY bat ke value
                sb.append("{\"key\":\"fake_").append(Long.toHexString(r.nextLong()))
                        .append("\",\"value\":").append(r.nextBoolean()).append('}');
                expected[2]++;
            } else if (roll < 95) {                           // key that, value khong phai boolean
                String bad = switch (r.nextInt(3)) {
                    case 0 -> "\"yes\"";
                    case 1 -> "null";
                    default -> "1";
                };
                sb.append("{\"key\":\"").append(validKeys.get(r.nextInt(validKeys.size())))
                        .append("\",\"value\":").append(bad).append('}');
                expected[3]++;
            } else {                                          // thieu key / key khong phai chuoi
                sb.append(r.nextBoolean() ? "{\"value\":true}" : "{\"key\":42,\"value\":false}");
                expected[3]++;
            }
        }
        return sb.append("]}").toString().getBytes(StandardCharsets.UTF_8);
    }
}
