package com.nequi.ticketing.domain.exception;

public class NotFoundException extends DomainException {

    public NotFoundException(String resource, String id) {
        super("%s with id '%s' was not found".formatted(resource, id));
    }
}
