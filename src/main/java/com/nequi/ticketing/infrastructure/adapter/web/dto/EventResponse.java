package com.nequi.ticketing.infrastructure.adapter.web.dto;

import com.nequi.ticketing.domain.model.Event;

import java.time.Instant;

public record EventResponse(String id, String name, Instant date, String venue, int totalCapacity, Instant createdAt) {

    public static EventResponse from(Event event) {
        return new EventResponse(event.id(), event.name(), event.date(), event.venue(), event.totalCapacity(),
                event.createdAt());
    }
}
