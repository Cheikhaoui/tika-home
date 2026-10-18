package com.tikahome.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Maps exceptions to RFC 9457 problem details ({@code application/problem+json}).
 * The parent class already handles Spring MVC errors (bad parameters, @Valid body failures, ...).
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** Postgres SQLSTATE for "exclusion constraint violated". */
    private static final String EXCLUSION_VIOLATION = "23P01";

    /** Invalid @Valid body: add a field -> message map so the booking form can highlight fields. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = ex.getBody();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    ProblemDetail handleBadRequest(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidBookingStateException.class)
    ProblemDetail handleInvalidBookingState(InvalidBookingStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BookingConflictException.class)
    ProblemDetail handleBookingConflict(BookingConflictException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("conflicts", ex.getConflicts());
        return problem;
    }

    @ExceptionHandler(BlockConflictException.class)
    ProblemDetail handleBlockConflict(BlockConflictException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("bookings", ex.getBookings());
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    ResponseEntity<ProblemDetail> handleTooManyAttempts(TooManyAttemptsException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setProperty("retryAfter", ex.getRetryAfter());
        long seconds = Math.max(1, Duration.between(Instant.now(), ex.getRetryAfter()).toSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(problem);
    }

    @ExceptionHandler(AssistantLimitException.class)
    ResponseEntity<ProblemDetail> handleAssistantLimit(AssistantLimitException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setProperty("limit", ex.getLimit());
        var response = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
        if (ex.getRetryAfter() != null) {
            problem.setProperty("retryAfter", ex.getRetryAfter());
            long seconds = Math.max(1, Duration.between(Instant.now(), ex.getRetryAfter()).toSeconds());
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
        }
        return response.body(problem);
    }

    @ExceptionHandler(AssistantUnavailableException.class)
    ProblemDetail handleAssistantUnavailable(AssistantUnavailableException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setProperty("reason", ex.getReason());
        return problem;
    }

    @ExceptionHandler(BookingRejectedException.class)
    ProblemDetail handleBookingRejected(BookingRejectedException ex) {
        var status = ex.isDateConflict() ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_CONTENT;
        var problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setProperty("issues", ex.getIssues());
        return problem;
    }

    /** Safety net: the database refused an overlapping booking that got past the service checks. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        if (EXCLUSION_VIOLATION.equals(sqlState(ex))) {
            var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "These dates are no longer available");
            problem.setProperty("issues", List.of("DATES_UNAVAILABLE"));
            return problem;
        }
        throw ex;
    }

    private static String sqlState(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState();
            }
        }
        return null;
    }
}
