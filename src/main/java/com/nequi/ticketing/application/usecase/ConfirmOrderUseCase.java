package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.exception.InvalidStateTransitionException;
import com.nequi.ticketing.domain.exception.NotFoundException;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.model.OrderStatus;
import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.port.OrderRepository;
import com.nequi.ticketing.domain.port.TicketRepository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;

/**
 * Confirms a RESERVED order before its deadline: tickets go RESERVED -> PENDING_CONFIRMATION -> SOLD
 * and the order becomes CONFIRMED. Confirming an already CONFIRMED order is a no-op (idempotent).
 */
public class ConfirmOrderUseCase {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final Clock clock;

    public ConfirmOrderUseCase(OrderRepository orderRepository, TicketRepository ticketRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.clock = clock;
    }

    public Mono<Order> execute(String orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Order", orderId)))
                .flatMap(order -> order.status() == OrderStatus.CONFIRMED ? Mono.just(order) : confirm(order));
    }

    private Mono<Order> confirm(Order order) {
        Instant now = clock.instant();
        if (order.status() != OrderStatus.RESERVED || !order.reservedUntil().isAfter(now)) {
            return Mono.error(new InvalidStateTransitionException(
                    "Order", order.id(), order.status().name(), OrderStatus.CONFIRMED.name()));
        }
        return ticketRepository.findByOrderId(order.id())
                .flatMap(this::sell)
                .then(orderRepository.update(order.confirmed(now)));
    }

    private Mono<Ticket> sell(Ticket ticket) {
        return ticketRepository.update(ticket.startConfirmation())
                .flatMap(pending -> ticketRepository.update(pending.sell()));
    }
}
