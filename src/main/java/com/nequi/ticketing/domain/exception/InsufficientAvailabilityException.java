package com.nequi.ticketing.domain.exception;

public class InsufficientAvailabilityException extends DomainException {

    public InsufficientAvailabilityException(String eventId, int requested, long available) {
        super("Event '%s' has %d tickets available but %d were requested".formatted(eventId, available, requested));
    }
}
