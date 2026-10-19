package com.prep.cs.counting;

import java.nio.ByteBuffer;

/**
 * Ban ghi gon cua MOT request sau khi phan loai: 32 byte co dinh, khong con key, khong con JSON.
 *   clientId 8 | epoch 4 | seq 8 | ts giay 4 | TRUE 2 | FALSE 2 | FAKE_KEY 2 | MALFORMED 2
 * Giu danh tinh request (clientId, epoch, seq) de phia sau KHU TRUNG duoc khi client retry; bo phan con lai.
 * Mot block (mot record Kafka) = so luong (4 byte) + n ban ghi.
 */
public final class RequestRecord {

    public static final int SIZE = 32;

    private RequestRecord() {}

    public static void write(ByteBuffer b, Decoded d) {
        b.putLong(d.clientId).putInt(d.epoch).putLong(d.seq).putInt((int) (d.tsMillis / 1000));
        for (int c : d.counts) b.putShort((short) c);   // <= 1000 item moi request: vua short
    }

    /** Doc vao Decoded dung lai; tsMillis tro thanh giay * 1000 (do phan giai cua ban ghi la giay). */
    public static void read(ByteBuffer b, Decoded into) {
        into.clientId = b.getLong();
        into.epoch = b.getInt();
        into.seq = b.getLong();
        into.tsMillis = (b.getInt() & 0xffffffffL) * 1000;
        int items = 0;
        for (int i = 0; i < 4; i++) {
            into.counts[i] = b.getShort();
            items += into.counts[i];
        }
        into.items = items;
    }
}
