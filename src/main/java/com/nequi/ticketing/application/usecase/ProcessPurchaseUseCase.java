package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.exception.ConcurrentModificationException;
import com.nequi.ticketing.domain.exception.NotFoundException;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.model.OrderStatus;
import com.nequi.ticketing.domain.model.PurchaseRequested;
import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.port.OrderRepository;
import com.nequi.ticketing.domain.port.TicketRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Queue consumer logic. Reserves tickets for a PENDING order using conditional writes so that two
 * consumers can never hold the same ticket, then moves the order to RESERVED (or REJECTED).
 *
 * <p>Idempotent by design: messages redelivered by SQS (at-least-once) for an order that is no
 * longer PENDING are acknowledged without side effects.
 */
public class ProcessPurchaseUseCase {

    /** Over-fetch factor to tolerate tickets grabbed by concurrent consumers between read and write. */
    private static final int CANDIDATE_FACTOR = 3;

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final Clock clock;
    private final Duration reservationTtl;

    public ProcessPurchaseUseCase(OrderRepository orderRepository, TicketRepository ticketRepository,
                                  Clock clock, Duration reservationTtl) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.clock = clock;
        this.reservationTtl = reservationTtl;
    }

    public Mono<Order> execute(PurchaseRequested message) {
        return orderRepository.findById(message.orderId())
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Order", message.orderId())))
                .flatMap(order -> order.status() == OrderStatus.PENDING ? reserve(order) : Mono.just(order));
    }

    private Mono<Order> reserve(Order order) {
        Instant now = clock.instant();
        Instant until = now.plus(reservationTtl);
        return ticketRepository.findAvailableByEvent(order.eventId(), order.quantity() * CANDIDATE_FACTOR)
                .concatMap(ticket -> tryReserve(ticket, order.id(), until))
                .take(order.quantity())
                .collectList()
                .flatMap(reserved -> reserved.size() == order.quantity()
                        ? orderRepository.update(order.reserved(ids(reserved), until, now))
                        : rollback(reserved).then(orderRepository.update(
                                order.rejected("Insufficient availability", now))));
    }

    private Mono<Ticket> tryReserve(Ticket ticket, String orderId, Instant until) {
        return ticketRepository.update(ticket.reserve(orderId, until))
                .onErrorResume(ConcurrentModificationException.class, e -> Mono.empty());
    }

    private Mono<Void> rollback(List<Ticket> reserved) {
        return Flux.fromIterable(reserved)
                .flatMap(ticket -> ticketRepository.update(ticket.release()))
                .then();
    }

    private static List<String> ids(List<Ticket> tickets) {
        return tickets.stream().map(Ticket::id).toList();
    }
}
