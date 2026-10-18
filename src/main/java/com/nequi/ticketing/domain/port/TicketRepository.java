package com.nequi.ticketing.domain.port;

import com.nequi.ticketing.domain.exception.ConcurrentModificationException;
import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.model.TicketStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface TicketRepository {

    Flux<Ticket> saveAll(Flux<Ticket> tickets);

    /**
     * Persists a state change using a conditional write on {@code version}.
     *
     * @return the updated ticket, or an error of {@link ConcurrentModificationException} when the
     *         stored version no longer matches {@code ticket.version() - 1}.
     */
    Mono<Ticket> update(Ticket ticket);

    Flux<Ticket> findAvailableByEvent(String eventId, int limit);

    Flux<Ticket> findByOrderId(String orderId);

    Flux<Ticket> findExpiredReservations(Instant now);

    Mono<Long> countByEventAndStatus(String eventId, TicketStatus status);
}
