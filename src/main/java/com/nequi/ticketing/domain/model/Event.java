package com.nequi.ticketing.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Event(
        String id,
        String name,
        Instant date,
        String venue,
        int totalCapacity,
        Instant createdAt
) {

    public Event {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name is required");
        }
        if (totalCapacity <= 0) {
            throw new IllegalArgumentException("Event capacity must be greater than zero");
        }
    }

    public static Event create(String name, Instant date, String venue, int totalCapacity, Instant now) {
        return new Event(UUID.randomUUID().toString(), name, date, venue, totalCapacity, now);
    }
}
