package com.prep.cs.counting;

/**
 * Bam mot chuoi ky tu thanh HAI so 64 bit (128 bit) trong MOT lan duyet, co seed bi mat.
 *   - lane 1 chon o trong bang bam (open addressing);
 *   - lane 2 de xac nhan: hai key khac nhau trung ca 128 bit co xac suat ~ n / 2^128, coi nhu khong xay ra.
 * Bam tren char (UTF-16) chu khong tren byte UTF-8: parser JSON streaming tra key duoi dang char[] co san,
 * bam thang tren do thi khong phai cap phat String hay byte[] o duong nong.
 * Moi vong lay 4 char = 64 bit, tron bang nhan + xoay, ket thuc bang fmix64 cua MurmurHash3.
 * Khong phai ham bam mat ma: seed bi mat chi lam ke tan cong kho doan; can chong tan cong that thi dung SipHash.
 */
public final class Hash128 {

    private static final long C1 = 0x87c37b91114253d5L, C2 = 0x4cf5ad432745937fL;

    private final long seed1, seed2;

    public Hash128(long seed1, long seed2) {
        this.seed1 = seed1;
        this.seed2 = seed2;
    }

    /** Ket qua dat vao out[0], out[1] de khong cap phat moi lan goi (duong nong goi ~20 lan moi request). */
    public void hash(char[] c, int off, int len, long[] out) {
        long h1 = seed1 ^ (len * C1), h2 = seed2 ^ (len * C2);
        int i = off, end = off + len;
        for (; i + 4 <= end; i += 4) {
            long w = c[i] | (long) c[i + 1] << 16 | (long) c[i + 2] << 32 | (long) c[i + 3] << 48;
            h1 = Long.rotateLeft(h1 ^ (w * C1), 31) * C2 + 0x52dce729;
            h2 = Long.rotateLeft(h2 ^ (w * C2), 33) * C1 + 0x38495ab5;
        }
        long tail = 0;
        for (int s = 0; i < end; i++, s += 16) tail |= (long) c[i] << s;
        h1 = Long.rotateLeft(h1 ^ (tail * C1), 31) * C2;
        h2 = Long.rotateLeft(h2 ^ (tail * C2), 33) * C1;
        out[0] = fmix64(h1 ^ h2 * 31);
        out[1] = fmix64(h2 + h1);
    }

    public void hash(String s, long[] out) {
        char[] c = s.toCharArray();
        hash(c, 0, c.length, out);
    }

    static long fmix64(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }
}
