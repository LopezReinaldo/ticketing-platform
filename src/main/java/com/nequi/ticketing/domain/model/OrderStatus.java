package com.nequi.ticketing.domain.model;

/**
 * Lifecycle of a purchase order.
 *
 * <pre>
 * PENDING ──► RESERVED ──► CONFIRMED
 *    │            │
 *    ▼            ▼
 * REJECTED     EXPIRED
 * </pre>
 */
public enum OrderStatus {
    /** Accepted by the API and queued; not yet processed by a consumer. */
    PENDING,
    /** Tickets are held for the customer until the reservation deadline. */
    RESERVED,
    /** Payment confirmed, tickets are SOLD. Final. */
    CONFIRMED,
    /** Not enough availability when processed. Final. */
    REJECTED,
    /** Reservation deadline passed without confirmation. Final. */
    EXPIRED;

    public boolean isFinal() {
        return this == CONFIRMED || this == REJECTED || this == EXPIRED;
    }

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == RESERVED || target == REJECTED;
            case RESERVED -> target == CONFIRMED || target == EXPIRED;
            case CONFIRMED, REJECTED, EXPIRED -> false;
        };
    }
}
