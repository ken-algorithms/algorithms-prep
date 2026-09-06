package com.prep.quarkus.infra;

import com.prep.quarkus.api.Dtos;
import com.prep.quarkus.app.TestRunService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Mapper exception -> HTTP, tra RFC 7807 problem+json.
 *
 * <p>Vi sao quan trong: khong co mapper thi mot {@code IllegalStateException} thanh 500 kem STACK
 * TRACE lot ra response. Do la (1) ro ri thong tin noi bo - van de bao mat, va (2) khach khong biet
 * phai sua gi. Sonar co rule rieng cho viec de stack trace lot ra ngoai.
 */
public final class ApiExceptionMappers {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMappers.class);
    private static final String MEDIA_TYPE = "application/problem+json";

    private ApiExceptionMappers() {}

    @Provider
    public static class RunNotFound implements ExceptionMapper<TestRunService.RunNotFoundException> {
        @Override
        public Response toResponse(TestRunService.RunNotFoundException ex) {
            // 404 la ket qua BINH THUONG, khong log level error (neu khong log se day rac).
            return problem(404, "Not Found", ex.getMessage(), List.of());
        }
    }

    @Provider
    public static class IllegalState implements ExceptionMapper<IllegalStateException> {
        @Override
        public Response toResponse(IllegalStateException ex) {
            // 409 Conflict: request hop le nhung trang thai hien tai khong cho phep.
            return problem(409, "Conflict", ex.getMessage(), List.of());
        }
    }

    @Provider
    public static class BadRequest implements ExceptionMapper<BadRequestException> {
        @Override
        public Response toResponse(BadRequestException ex) {
            return problem(400, "Bad Request", ex.getMessage(), List.of());
        }
    }

    /** Bean Validation -> 400 kem DANH SACH loi, khong chi loi dau tien. */
    @Provider
    public static class Validation implements ExceptionMapper<ConstraintViolationException> {
        @Override
        public Response toResponse(ConstraintViolationException ex) {
            List<String> errors = ex.getConstraintViolations().stream()
                    .map(Validation::describe)
                    .sorted()
                    .toList();
            return problem(400, "Bad Request", "request validation failed", errors);
        }

        private static String describe(ConstraintViolation<?> violation) {
            return violation.getPropertyPath() + ": " + violation.getMessage();
        }
    }

    /**
     * Luoi cuoi: KHONG bao gio de stack trace lot ra client.
     *
     * <p><b>BUG THAT DA GAP KHI CHAY TEST — dang ghi lai vi day la bai hoc dat gia:</b>
     * ban dau class nay bat {@code RuntimeException} roi tra 500 cho MOI truong hop. Nhung
     * {@code jakarta.ws.rs.WebApplicationException} (va cac subclass nhu {@code NotSupportedException}
     * = 415, {@code NotFoundException} = 404, {@code NotAllowedException} = 405) CUNG la
     * RuntimeException — va chung DA MANG SAN status code dung.
     *
     * <p>Ket qua: mot request thieu header Content-Type le ra phai nhan <b>415</b> thi lai nhan
     * <b>500</b>. Client khong biet minh sai gi, con on-call thi thay alert 5xx gia. Mot catch-all
     * qua rong bien loi CUA CLIENT thanh loi CUA SERVER.
     *
     * <p>Sua: neu exception da la WebApplicationException thi TON TRONG status cua no.
     * Nguyen tac chung: catch-all chi duoc xu ly nhung gi THUC SU khong luong truoc.
     */
    @Provider
    public static class Unexpected implements ExceptionMapper<RuntimeException> {
        @Override
        public Response toResponse(RuntimeException ex) {
            if (ex instanceof WebApplicationException web) {
                int status = web.getResponse().getStatus();
                // 4xx = loi cua client -> khong log error (neu khong log day rac va bao dong gia).
                if (status < 500) {
                    LOG.debugf("client error %d: %s", status, ex.getMessage());
                } else {
                    LOG.error("server error", ex);
                }
                return problem(status, reasonFor(status), ex.getMessage(), List.of());
            }
            // Log DAY DU o server (co stack trace) nhung tra ve client thong diep chung chung.
            LOG.error("unhandled exception", ex);
            return problem(500, "Internal Server Error", "unexpected error", List.of());
        }

        private static String reasonFor(int status) {
            Response.Status resolved = Response.Status.fromStatusCode(status);
            return resolved == null ? "Error" : resolved.getReasonPhrase();
        }
    }

    private static Response problem(int status, String title, String detail, List<String> errors) {
        return Response.status(status)
                .type(MEDIA_TYPE)
                .entity(new Dtos.Problem("about:blank", title, status, detail, errors))
                .build();
    }
}
