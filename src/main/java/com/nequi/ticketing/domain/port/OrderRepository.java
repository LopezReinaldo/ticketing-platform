package com.nequi.ticketing.domain.port;

import com.nequi.ticketing.domain.exception.ConcurrentModificationException;
import com.nequi.ticketing.domain.model.Order;
import reactor.core.publisher.Mono;

public interface OrderRepository {

    Mono<Order> save(Order order);

    /**
     * Persists a state change using a conditional write on {@code version}.
     * Emits {@link ConcurrentModificationException} when the stored version does not match.
     */
    Mono<Order> update(Order order);

    Mono<Order> findById(String orderId);

    Mono<Order> findByIdempotencyKey(String idempotencyKey);
}
