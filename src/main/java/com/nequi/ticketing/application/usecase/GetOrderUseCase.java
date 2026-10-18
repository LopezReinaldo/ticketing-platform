package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.exception.NotFoundException;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.port.OrderRepository;
import reactor.core.publisher.Mono;

public class GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Mono<Order> execute(String orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Order", orderId)));
    }
}
