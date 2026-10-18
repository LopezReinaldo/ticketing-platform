package com.nequi.ticketing.infrastructure.adapter.web.dto;

import com.nequi.ticketing.domain.model.Availability;

public record AvailabilityResponse(String eventId, int totalCapacity, long available, long reserved, long sold) {

    public static AvailabilityResponse from(Availability a) {
        return new AvailabilityResponse(a.eventId(), a.totalCapacity(), a.available(), a.reserved(), a.sold());
    }
}
