package com.nequi.ticketing.domain.exception;

/**
 * Raised by repositories when an optimistic-lock / conditional write fails because another
 * process modified the same aggregate. Use cases treat it as a retryable error.
 */
public class ConcurrentModificationException extends DomainException {

    public ConcurrentModificationException(String resource, String id) {
        super("%s '%s' was modified concurrently".formatted(resource, id));
    }
}
