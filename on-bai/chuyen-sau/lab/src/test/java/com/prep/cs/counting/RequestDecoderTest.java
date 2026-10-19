package com.prep.cs.counting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class RequestDecoderTest {

    static final Hash128 HASH = new Hash128(0x9E3779B97F4A7C15L, 0xC2B2AE3D27D4EB4FL);
    static final List<String> KEYS = List.of("checkout_v2", "search", "login", "đăng_nhập");
    static final KeyRegistry REG = KeyRegistry.of(KEYS, HASH, 1);

    final RequestDecoder decoder = new RequestDecoder();
    final Decoded d = new Decoded();

    void decode(String json) {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        decoder.decode(b, 0, b.length, REG, d);
    }

    @Test
    void classifiesKeyFirstThenValue() {
        decode("""
                {"clientId":42,"epoch":7,"seq":17,"ts":1792390123456,"items":[
                  {"key":"checkout_v2","value":true},
                  {"key":"search","value":false},
                  {"key":"nope","value":true},
                  {"key":"nope","value":"yes"},
                  {"key":"login","value":"yes"},
                  {"key":"login","value":null},
                  {"key":"login","value":1},
                  {"value":true},
                  {"key":42,"value":false},
                  "khong-phai-object",
                  {"value":true,"key":"đăng_nhập"}
                ]}""");
        System.out.println("[LAB] " + d);
        assertThat(d.clientId).isEqualTo(42);
        assertThat(d.seq).isEqualTo(17);
        assertThat(d.items).isEqualTo(11);
        assertThat(d.count(Outcome.TRUE)).isEqualTo(2);      // checkout_v2, đăng_nhập (value truoc key van dung)
        assertThat(d.count(Outcome.FALSE)).isEqualTo(1);     // search
        assertThat(d.count(Outcome.FAKE_KEY)).isEqualTo(2);  // nope x2: key gia thi khong xet value
        assertThat(d.count(Outcome.MALFORMED)).isEqualTo(6); // "yes", null, 1, thieu key, key so, khong phai object
    }

    @Test
    void ignoresUnknownFieldsAtAnyDepth() {
        decode("""
                {"meta":{"sdk":"2.1","tags":["a","b"]},"clientId":1,"epoch":1,"seq":0,"ts":5,
                 "items":[{"key":"search","value":true,"extra":{"x":[1,2,{"y":3}]}}]}""");
        assertThat(d.count(Outcome.TRUE)).isEqualTo(1);
    }

    @Test
    void rejectsBadRequests() {
        assertThatThrownBy(() -> decode("{\"epoch\":1,\"seq\":0,\"ts\":5,\"items\":[{\"key\":\"a\",\"value\":true}]}"))
                .isInstanceOf(RequestDecoder.BadRequest.class).hasMessageContaining("thieu");
        assertThatThrownBy(() -> decode("{\"clientId\":1,\"epoch\":1,\"seq\":0,\"ts\":5,\"items\":[]}"))
                .hasMessageContaining("rong");
        assertThatThrownBy(() -> decode("{\"clientId\":1,\"epoch\":1,\"seq\":0,\"ts\":5,\"items\":[{\"key\":\"a\""))
                .isInstanceOf(RequestDecoder.BadRequest.class);
        String big = IntStream.range(0, 1001).mapToObj(i -> "{\"key\":\"search\",\"value\":true}")
                .collect(java.util.stream.Collectors.joining(",", "{\"clientId\":1,\"epoch\":1,\"seq\":0,\"ts\":5,\"items\":[", "]}"));
        assertThatThrownBy(() -> decode(big)).hasMessageContaining("1000");
    }

    @Test
    void agreesWithIndependentOracleOn20000RandomRequests() {
        List<String> keys = IntStream.range(0, 5_000).mapToObj(i -> "feature_" + i).toList();
        KeyRegistry reg = KeyRegistry.of(keys, HASH, 1);
        LoadGen gen = new LoadGen(keys);
        SplittableRandom r = new SplittableRandom(7);
        long[] expected = new long[4], got = new long[4];
        for (int i = 0; i < 20_000; i++) {
            byte[] body = gen.body(i % 100, 1, i, 1_792_390_000_000L + i, 20, r, expected);
            decoder.decode(body, 0, body.length, reg, d);
            for (int k = 0; k < 4; k++) got[k] += d.counts[k];
        }
        System.out.printf("[LAB] 20.000 request x 20 item: oracle %s, decoder %s%n",
                java.util.Arrays.toString(expected), java.util.Arrays.toString(got));
        assertThat(got).containsExactly(expected);
    }
}
