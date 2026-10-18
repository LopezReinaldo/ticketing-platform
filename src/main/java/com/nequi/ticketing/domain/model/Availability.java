package com.nequi.ticketing.domain.model;

/** Real-time inventory snapshot for an event. */
public record Availability(
        String eventId,
        int totalCapacity,
        long available,
        long reserved,
        long sold
) {
}
