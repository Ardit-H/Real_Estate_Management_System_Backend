package com.realestate.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * UnauthorizedException — hidhet kur autentikimi dështon.
 * HTTP 401 Unauthorized
 *
 * Shembuj përdorimi:
 *   - Kredenciale të gabuara gjatë login
 *   - Token i skaduar ose i pavlefshëm
 *   - Refresh token i revokuar
 *   - Llogaria e çaktivizuar
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}