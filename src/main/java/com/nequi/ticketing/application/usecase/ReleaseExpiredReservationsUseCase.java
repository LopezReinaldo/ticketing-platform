package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.exception.DomainException;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.model.OrderStatus;
import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.port.OrderRepository;
import com.nequi.ticketing.domain.port.TicketRepository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;

/**
 * Periodic job: returns expired RESERVED tickets to inventory and marks their orders as EXPIRED.
 * Conditional-write failures are ignored because they mean another instance already handled the item.
 *
 * @return number of tickets released
 */
public class ReleaseExpiredReservationsUseCase {

    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;

    public ReleaseExpiredReservationsUseCase(TicketRepository ticketRepository, OrderRepository orderRepository,
                                             Clock clock) {
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    public Mono<Long> execute() {
        Instant now = clock.instant();
        return ticketRepository.findExpiredReservations(now)
                .flatMap(ticket -> release(ticket, now))
                .count();
    }

    private Mono<Ticket> release(Ticket ticket, Instant now) {
        String orderId = ticket.orderId();
        return ticketRepository.update(ticket.release())
                .flatMap(released -> expireOrder(orderId, now).thenReturn(released))
                .onErrorResume(DomainException.class, e -> Mono.empty());
    }

    private Mono<Order> expireOrder(String orderId, Instant now) {
        return orderRepository.findById(orderId)
                .filter(order -> order.status() == OrderStatus.RESERVED)
                .flatMap(order -> orderRepository.update(order.expired(now)))
                .onErrorResume(DomainException.class, e -> Mono.empty());
    }
}
