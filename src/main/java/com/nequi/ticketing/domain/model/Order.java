package com.nequi.ticketing.domain.model;

import com.nequi.ticketing.domain.exception.InvalidStateTransitionException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Purchase order. {@code idempotencyKey} is supplied by the client so retried requests
 * never create duplicated orders.
 */
public record Order(
        String id,
        String eventId,
        String customerId,
        int quantity,
        String idempotencyKey,
        OrderStatus status,
        List<String> ticketIds,
        Instant reservedUntil,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        long version
) {

    public Order {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be greater than zero");
        }
        ticketIds = ticketIds == null ? List.of() : List.copyOf(ticketIds);
    }

    public static Order create(String eventId, String customerId, int quantity, String idempotencyKey, Instant now) {
        return new Order(UUID.randomUUID().toString(), eventId, customerId, quantity, idempotencyKey,
                OrderStatus.PENDING, List.of(), null, null, now, now, 0L);
    }

    public Order reserved(List<String> ticketIds, Instant until, Instant now) {
        return transition(OrderStatus.RESERVED, ticketIds, until, null, now);
    }

    public Order confirmed(Instant now) {
        return transition(OrderStatus.CONFIRMED, ticketIds, null, null, now);
    }

    public Order rejected(String reason, Instant now) {
        return transition(OrderStatus.REJECTED, List.of(), null, reason, now);
    }

    public Order expired(Instant now) {
        return transition(OrderStatus.EXPIRED, List.of(), null, "Reservation expired", now);
    }

    private Order transition(OrderStatus target, List<String> tickets, Instant until, String reason, Instant now) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("Order", id, status.name(), target.name());
        }
        return new Order(id, eventId, customerId, quantity, idempotencyKey, target, tickets, until, reason,
                createdAt, now, version + 1);
    }
}
