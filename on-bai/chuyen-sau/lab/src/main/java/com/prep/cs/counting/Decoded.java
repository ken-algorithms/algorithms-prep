package com.prep.cs.counting;

import java.util.Arrays;

/**
 * Mot request da giai ma va phan loai, o dang gon nhat can de dem: ai gui (clientId, epoch, seq), luc nao (ts),
 * va 4 con dem. Doi tuong DUNG LAI (moi luong mot cai), khong cap phat moi request.
 */
public final class Decoded {
    public long clientId;
    public int epoch;        // moi lan tien trinh client khoi dong lai: epoch moi, seq dem lai tu 0
    public long seq;         // so thu tu request cua client trong epoch; retry gui lai DUNG seq cu
    public long tsMillis;    // event time do client gan
    public int items;
    public final int[] counts = new int[4];   // chi so = Outcome.ordinal()

    void reset() {
        clientId = -1;
        epoch = -1;
        seq = -1;
        tsMillis = -1;
        items = 0;
        Arrays.fill(counts, 0);
    }

    public int count(Outcome o) {
        return counts[o.ordinal()];
    }

    @Override
    public String toString() {
        return "client=" + clientId + " epoch=" + epoch + " seq=" + seq + " ts=" + tsMillis
                + " TRUE=" + counts[0] + " FALSE=" + counts[1] + " FAKE_KEY=" + counts[2] + " MALFORMED=" + counts[3];
    }
}
