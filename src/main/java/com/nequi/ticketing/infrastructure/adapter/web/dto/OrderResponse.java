package com.nequi.ticketing.infrastructure.adapter.web.dto;

import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.model.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String eventId,
        String customerId,
        int quantity,
        OrderStatus status,
        List<String> ticketIds,
        Instant reservedUntil,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.id(), o.eventId(), o.customerId(), o.quantity(), o.status(), o.ticketIds(),
                o.reservedUntil(), o.failureReason(), o.createdAt(), o.updatedAt());
    }
}
