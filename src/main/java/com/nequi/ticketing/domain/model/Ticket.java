package com.nequi.ticketing.domain.model;

import com.nequi.ticketing.domain.exception.InvalidStateTransitionException;

import java.time.Instant;
import java.util.UUID;

/**
 * Inventory unit. The {@code version} field backs optimistic locking in the persistence layer:
 * every state change produces a new instance with {@code version + 1}.
 */
public record Ticket(
        String id,
        String eventId,
        TicketStatus status,
        String orderId,
        Instant reservedUntil,
        long version
) {

    public static Ticket available(String eventId) {
        return new Ticket(UUID.randomUUID().toString(), eventId, TicketStatus.AVAILABLE, null, null, 0L);
    }

    public Ticket reserve(String orderId, Instant until) {
        return transition(TicketStatus.RESERVED, orderId, until);
    }

    public Ticket startConfirmation() {
        return transition(TicketStatus.PENDING_CONFIRMATION, orderId, reservedUntil);
    }

    public Ticket sell() {
        return transition(TicketStatus.SOLD, orderId, null);
    }

    public Ticket release() {
        return transition(TicketStatus.AVAILABLE, null, null);
    }

    public boolean isReservationExpired(Instant now) {
        return status == TicketStatus.RESERVED && reservedUntil != null && !reservedUntil.isAfter(now);
    }

    private Ticket transition(TicketStatus target, String newOrderId, Instant newReservedUntil) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("Ticket", id, status.name(), target.name());
        }
        return new Ticket(id, eventId, target, newOrderId, newReservedUntil, version + 1);
    }
}
