package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.exception.NotFoundException;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.model.PurchaseRequested;
import com.nequi.ticketing.domain.port.EventRepository;
import com.nequi.ticketing.domain.port.OrderRepository;
import com.nequi.ticketing.domain.port.PurchaseQueuePublisher;
import reactor.core.publisher.Mono;

import java.time.Clock;

/**
 * Accepts a purchase request: persists a PENDING order, enqueues it for asynchronous processing and
 * returns immediately. Requests carrying an already-known idempotency key return the existing order.
 */
public class PlaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final PurchaseQueuePublisher publisher;
    private final Clock clock;

    public PlaceOrderUseCase(OrderRepository orderRepository, EventRepository eventRepository,
                             PurchaseQueuePublisher publisher, Clock clock) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.publisher = publisher;
        this.clock = clock;
    }

    public Mono<Order> execute(String eventId, String customerId, int quantity, String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .switchIfEmpty(Mono.defer(() -> createAndEnqueue(eventId, customerId, quantity, idempotencyKey)));
    }

    private Mono<Order> createAndEnqueue(String eventId, String customerId, int quantity, String idempotencyKey) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Event", eventId)))
                .map(event -> Order.create(event.id(), customerId, quantity, idempotencyKey, clock.instant()))
                .flatMap(orderRepository::save)
                .flatMap(order -> publisher.publish(PurchaseRequested.from(order)).thenReturn(order));
    }
}
