package com.realestate.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Helper factory — p.sh. ResourceNotFoundException.of("Property", 42)
    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException(
                resource + " me id=" + id + " nuk u gjet"
        );
    }
}