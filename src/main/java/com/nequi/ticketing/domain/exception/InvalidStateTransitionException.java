package com.nequi.ticketing.domain.exception;

public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String resource, String id, String from, String to) {
        super("%s '%s' cannot transition from %s to %s".formatted(resource, id, from, to));
    }
}
