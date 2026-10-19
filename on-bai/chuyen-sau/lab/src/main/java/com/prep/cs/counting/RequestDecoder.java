package com.prep.cs.counting;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Doc body JSON cua request bang parser STREAMING (token mot) va phan loai tung cap key-value ngay khi doc.
 * Khong dung object trung gian (khong POJO, khong List, khong String cho key): o 10 trieu request/giay,
 * moi byte cap phat them nhan voi 10 trieu.
 *
 *   {"clientId":42,"epoch":1792390000,"seq":17,"ts":1792390123456,
 *    "items":[{"key":"checkout_v2","value":true},{"key":"nope","value":false},{"key":"search","value":"yes"}]}
 *
 * Luat phan loai (giong bai 04): KEY TRUOC, VALUE SAU.
 *   key thieu / khong phai chuoi            -> MALFORMED
 *   key khong co trong registry             -> FAKE_KEY (bat ke value)
 *   value true / false                      -> TRUE / FALSE
 *   value khac (null, "yes", 1, object...)  -> MALFORMED (khong gop vao FALSE)
 */
public final class RequestDecoder {

    public static final int MAX_ITEMS = 1_000;

    private final JsonFactory factory = new JsonFactory();
    private final long[] h = new long[2];          // moi luong mot decoder: khong chia se mang nay

    public static final class BadRequest extends RuntimeException {
        public BadRequest(String msg) {
            super(msg, null, false, false);            // khong chup stack trace: re hon ~100 lan
        }
    }

    public void decode(byte[] body, int off, int len, KeyRegistry registry, Decoded out) {
        out.reset();
        try (JsonParser p = factory.createParser(body, off, len)) {
            if (p.nextToken() != JsonToken.START_OBJECT) throw new BadRequest("body phai la object");
            boolean sawItems = false;
            while (p.nextToken() == JsonToken.FIELD_NAME) {
                String field = p.currentName();            // ten truong duoc Jackson intern: khong cap phat
                JsonToken t = p.nextToken();
                switch (field) {
                    case "clientId" -> out.clientId = longValue(p, t, field);
                    case "epoch" -> out.epoch = (int) longValue(p, t, field);
                    case "seq" -> out.seq = longValue(p, t, field);
                    case "ts" -> out.tsMillis = longValue(p, t, field);
                    case "items" -> {
                        if (t != JsonToken.START_ARRAY) throw new BadRequest("items phai la mang");
                        readItems(p, registry, out);
                        sawItems = true;
                    }
                    default -> p.skipChildren();           // truong la: bo qua ca cay con
                }
            }
            if (out.clientId < 0 || out.epoch < 0 || out.seq < 0 || out.tsMillis < 0 || !sawItems) {
                throw new BadRequest("thieu clientId/epoch/seq/ts/items");
            }
            if (out.items == 0) throw new BadRequest("items rong");
        } catch (IOException e) {
            // khong tra nguyen thong diep cua Jackson: dai, nhieu dong (vo JSON cua response), lo chi tiet noi bo
            long col = e instanceof JsonProcessingException j && j.getLocation() != null ? j.getLocation().getColumnNr() : -1;
            throw new BadRequest("JSON hong o cot " + col);
        }
    }

    private void readItems(JsonParser p, KeyRegistry registry, Decoded out) throws IOException {
        JsonToken item;
        while ((item = p.nextToken()) != JsonToken.END_ARRAY) {
            if (item == null) throw new BadRequest("JSON bi cat giua chung");
            if (++out.items > MAX_ITEMS) throw new BadRequest("qua " + MAX_ITEMS + " item");
            if (item != JsonToken.START_OBJECT) {     // phan tu khong phai object: van dem, la MALFORMED
                p.skipChildren();
                out.counts[Outcome.MALFORMED.ordinal()]++;
                continue;
            }
            int key = 0;                 // 0 thieu, 1 hop le, 2 gia, 3 khong phai chuoi
            JsonToken value = null;
            while (p.nextToken() == JsonToken.FIELD_NAME) {
                String f = p.currentName();
                JsonToken t = p.nextToken();
                if (f.equals("key")) {
                    if (t == JsonToken.VALUE_STRING) {
                        // char[] nam san trong buffer cua parser: bam thang, khong tao String
                        registry.hash().hash(p.getTextCharacters(), p.getTextOffset(), p.getTextLength(), h);
                        key = registry.contains(h[0], h[1]) ? 1 : 2;
                    } else {
                        key = 3;
                        p.skipChildren();
                    }
                } else if (f.equals("value")) {
                    value = t;
                    p.skipChildren();
                } else {
                    p.skipChildren();
                }
            }
            Outcome o;
            if (key == 0 || key == 3) o = Outcome.MALFORMED;
            else if (key == 2) o = Outcome.FAKE_KEY;
            else if (value == JsonToken.VALUE_TRUE) o = Outcome.TRUE;
            else if (value == JsonToken.VALUE_FALSE) o = Outcome.FALSE;
            else o = Outcome.MALFORMED;
            out.counts[o.ordinal()]++;
        }
    }

    private static long longValue(JsonParser p, JsonToken t, String field) throws IOException {
        if (t != JsonToken.VALUE_NUMBER_INT) throw new BadRequest(field + " phai la so nguyen");
        long v = p.getLongValue();
        if (v < 0) throw new BadRequest(field + " am");
        return v;
    }
}
