package com.realestate.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class SchemaNotProvisionedException extends RuntimeException {

    public SchemaNotProvisionedException(Long tenantId) {
        super("Schema për tenant_id=" + tenantId +
                " nuk është gati ende. Provo sërish pas disa sekondash.");
    }
}