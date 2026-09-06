package com.prep.spring.infra;

import com.prep.spring.app.SpringTestRunService;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Spring 6 co san {@link ProblemDetail} (RFC 7807) - khong phai tu dinh nghia record Problem nhu
 * module 02 (Quarkus chua co tuong duong built-in).
 *
 * <p>Day la mot diem Spring tien hon that su, dang neu ra khi so sanh hai framework.
 *
 * <p>Chu y KHONG viet {@code @ExceptionHandler(Exception.class)} tra 500 cho tat ca — day dung la
 * bug toi da gap o module 02: no se nuot ca cac exception cua Spring MVC da mang san status
 * (404/405/415). Spring da co {@code ResponseEntityExceptionHandler} lo phan do; ta chi bat
 * exception CUA MINH.
 */
@RestControllerAdvice
public class SpringExceptionHandler {

    @ExceptionHandler(SpringTestRunService.RunNotFoundException.class)
    public ProblemDetail handleNotFound(SpringTestRunService.RunNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SpringTestRunService.RunClosedException.class)
    public ProblemDetail handleConflict(SpringTestRunService.RunClosedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** @Valid tren @RequestBody that bai -> gom CA CUM loi. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBodyValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .toList();
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "request validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    /** @Min/@Max tren @RequestParam that bai. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleParamValidation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "parameter validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }
}
