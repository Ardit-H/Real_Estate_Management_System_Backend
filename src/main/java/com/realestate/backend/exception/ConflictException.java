package com.realestate.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ConflictException — hidhet kur një resource ekziston tashmë.
 * HTTP 409 Conflict
 *
 * Shembuj përdorimi:
 *   - Email ekziston tashmë gjatë regjistrimit
 *   - Slug i tenant-it është i zënë
 *   - Kontratë aktive ekziston për këtë pronë
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
