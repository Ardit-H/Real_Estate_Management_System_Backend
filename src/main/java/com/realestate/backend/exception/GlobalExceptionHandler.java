package com.realestate.backend.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — kap të gjitha exception-et dhe
 * i kthen si JSON të strukturuar.
 *
 * Çdo response ka formatin:
 * {
 *   "status":    404,
 *   "error":     "Not Found",
 *   "message":   "Property me id=5 nuk u gjet",
 *   "path":      "/api/properties/5",
 *   "timestamp": "2025-01-15T10:30:00"
 * }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 409 Conflict ──────────────────────────────────────
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex, WebRequest request) {

        log.warn("Conflict: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // ─── 401 Unauthorized ──────────────────────────────────
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, WebRequest request) {

        log.warn("Unauthorized: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    // ─── 401 nga Spring Security ───────────────────────────
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, WebRequest request) {

        return build(HttpStatus.UNAUTHORIZED, "Autentikimi dështoi", request);
    }

    // ─── 403 Forbidden ─────────────────────────────────────
    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(
            RuntimeException ex, WebRequest request) {

        log.warn("Forbidden: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Nuk ke leje për këtë veprim", request);
    }

    // ─── 404 Not Found ─────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // ─── 400 Bad Request ───────────────────────────────────
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, WebRequest request) {

        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // ─── 422 Invalid State ─────────────────────────────────
    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(
            InvalidStateException ex, WebRequest request) {

        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    // ─── 503 Schema Not Provisioned ────────────────────────
    @ExceptionHandler(SchemaNotProvisionedException.class)
    public ResponseEntity<ErrorResponse> handleSchemaNotProvisioned(
            SchemaNotProvisionedException ex, WebRequest request) {

        log.error("Schema not provisioned: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    // ─── 400 Validation errors (@Valid dështon) ────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field   = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        ValidationErrorResponse body = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validimi dështoi",
                fieldErrors,
                extractPath(request),
                LocalDateTime.now()
        );

        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ─── 500 Gabime të papritura ───────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, WebRequest request) {

        log.error("Gabim i papritur: {}", ex.getMessage(), ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Gabim i brendshëm i serverit. Provo sërish.",
                request
        );
    }

    // ─── Helpers ───────────────────────────────────────────
    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String message, WebRequest request) {

        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                extractPath(request),
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    // ─── Response records ──────────────────────────────────
    public record ErrorResponse(
            int             status,
            String          error,
            String          message,
            String          path,
            LocalDateTime   timestamp
    ) {}

    public record ValidationErrorResponse(
            int                     status,
            String                  message,
            Map<String, String>     errors,
            String                  path,
            LocalDateTime           timestamp
    ) {}
}