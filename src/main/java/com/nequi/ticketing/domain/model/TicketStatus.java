package com.nequi.ticketing.domain.model;

import java.util.Set;

/**
 * Lifecycle of a single ticket. Transitions are validated through {@link #canTransitionTo(TicketStatus)}
 * so every state change is explicit and auditable.
 */
public enum TicketStatus {
    AVAILABLE,
    RESERVED,
    PENDING_CONFIRMATION,
    SOLD,
    COMPLIMENTARY;

    private static final Set<TicketStatus> FINAL_STATES = Set.of(SOLD, COMPLIMENTARY);

    public boolean isFinal() {
        return FINAL_STATES.contains(this);
    }

    /** Only {@code SOLD} counts as revenue; {@code COMPLIMENTARY} is final but not accounted as a sale. */
    public boolean countsAsSale() {
        return this == SOLD;
    }

    /** A ticket is unavailable to the public while held or already in a final state. */
    public boolean consumesInventory() {
        return this != AVAILABLE;
    }

    public boolean canTransitionTo(TicketStatus target) {
        return switch (this) {
            case AVAILABLE -> target == RESERVED || target == COMPLIMENTARY;
            case RESERVED -> target == PENDING_CONFIRMATION || target == AVAILABLE;
            case PENDING_CONFIRMATION -> target == SOLD || target == AVAILABLE;
            case SOLD, COMPLIMENTARY -> false;
        };
    }
}
