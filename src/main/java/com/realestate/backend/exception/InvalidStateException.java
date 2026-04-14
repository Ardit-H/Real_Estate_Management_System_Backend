package com.realestate.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }

    // p.sh. InvalidStateException.forProperty(1L, "SOLD", "AVAILABLE")
    public static InvalidStateException forProperty(Long id,
                                                    String current,
                                                    String required) {
        return new InvalidStateException(
                "Prona #" + id + " është '" + current +
                        "', por duhet të jetë '" + required + "' për këtë veprim"
        );
    }
}