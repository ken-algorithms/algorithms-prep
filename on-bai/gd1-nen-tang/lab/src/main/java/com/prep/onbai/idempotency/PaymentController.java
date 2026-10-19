package com.prep.onbai.idempotency;

import com.prep.onbai.idempotency.PaymentService.Outcome;
import com.prep.onbai.idempotency.PaymentService.PaymentRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    /** Idempotency-Key bat buoc voi POST tao tien: client sinh MOT lan cho MOT y dinh, dung lai khi retry. */
    @PostMapping("/payments")
    public ResponseEntity<String> create(@RequestHeader(value = "Idempotency-Key", required = false) String key,
                                         @RequestHeader(value = "X-Client-Id", defaultValue = "demo") String client,
                                         @RequestBody PaymentRequest body) {
        if (key == null || key.isBlank() || key.length() > 255) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"idempotency_key_required\"}");
        }
        Outcome o = payments.pay(client, key, body);
        ResponseEntity.BodyBuilder res = ResponseEntity.status(o.code()).contentType(MediaType.APPLICATION_JSON);
        if (o.replayed()) {
            res.header("Idempotent-Replayed", "true");
        }
        if (o.retryAfterMs() > 0) {
            res.header("Retry-After", String.valueOf(Math.max(1, (o.retryAfterMs() + 999) / 1000)));
        }
        return res.body(o.body());
    }
}
