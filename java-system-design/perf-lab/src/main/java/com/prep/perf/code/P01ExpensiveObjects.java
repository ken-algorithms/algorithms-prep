package com.prep.perf.code;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * P01 - Tao lai object DAT o moi request.
 *
 * <p>ObjectMapper, DateTimeFormatter, Pattern, HttpClient, JAXBContext, MessageDigest... deu ton
 * chi phi khoi tao lon (doc annotation, build cache, parse pattern) nhung re khi DUNG LAI. Viet
 * {@code new ObjectMapper()} trong controller/service thi chi phi khoi tao bi nhan voi QPS.
 *
 * <p>ObjectMapper va DateTimeFormatter thread-safe sau khi cau hinh xong -> mot instance dung chung
 * cho ca JVM. (SimpleDateFormat thi KHONG thread-safe - dung chung la sai ket qua, tao moi la cham;
 * cach dung la bo han, chuyen sang DateTimeFormatter.)
 */
public final class P01ExpensiveObjects {

    private P01ExpensiveObjects() {}

    public record Transfer(String id, String fromAccount, String toAccount, long amountMinor,
                           String currency, long createdAtEpochMs) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    /** XAU: moi lan goi build lai toan bo cache serializer cua Jackson. */
    public static String toJsonBad(Transfer t) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(t);
    }

    /** SUA: mot ObjectMapper cho ca JVM (trong Spring: inject bean ObjectMapper co san). */
    public static String toJsonGood(Transfer t) throws JsonProcessingException {
        return MAPPER.writeValueAsString(t);
    }

    /** XAU: parse lai pattern moi lan. */
    public static String formatBad(Instant at) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC).format(at);
    }

    /** SUA: formatter static final. */
    public static String formatGood(Instant at) {
        return FMT.format(at);
    }
}
