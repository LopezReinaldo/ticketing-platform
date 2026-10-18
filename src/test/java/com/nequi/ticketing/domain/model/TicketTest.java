package com.nequi.ticketing.domain.model;

import com.nequi.ticketing.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void followsReserveConfirmSellLifecycle() {
        Ticket available = Ticket.available("event-1");
        Ticket reserved = available.reserve("order-1", NOW.plusSeconds(600));
        Ticket pending = reserved.startConfirmation();
        Ticket sold = pending.sell();

        assertThat(reserved.status()).isEqualTo(TicketStatus.RESERVED);
        assertThat(reserved.orderId()).isEqualTo("order-1");
        assertThat(pending.status()).isEqualTo(TicketStatus.PENDING_CONFIRMATION);
        assertThat(sold.status()).isEqualTo(TicketStatus.SOLD);
        assertThat(sold.version()).isEqualTo(3L);
        assertThat(sold.reservedUntil()).isNull();
    }

    @Test
    void soldTicketIsFinal() {
        Ticket sold = Ticket.available("event-1").reserve("o", NOW).startConfirmation().sell();

        assertThatThrownBy(sold::release).isInstanceOf(InvalidStateTransitionException.class);
        assertThat(TicketStatus.SOLD.isFinal()).isTrue();
        assertThat(TicketStatus.SOLD.countsAsSale()).isTrue();
        assertThat(TicketStatus.COMPLIMENTARY.countsAsSale()).isFalse();
    }

    @Test
    void cannotSellAnAvailableTicketDirectly() {
        assertThatThrownBy(() -> Ticket.available("event-1").sell())
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void detectsExpiredReservation() {
        Ticket reserved = Ticket.available("event-1").reserve("order-1", NOW);

        assertThat(reserved.isReservationExpired(NOW)).isTrue();
        assertThat(reserved.isReservationExpired(NOW.minusSeconds(1))).isFalse();
        assertThat(reserved.release().isReservationExpired(NOW)).isFalse();
    }
}
