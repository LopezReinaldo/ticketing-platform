package com.nequi.ticketing.infrastructure.adapter.web;

import com.nequi.ticketing.application.usecase.ConfirmOrderUseCase;
import com.nequi.ticketing.application.usecase.GetOrderUseCase;
import com.nequi.ticketing.application.usecase.PlaceOrderUseCase;
import com.nequi.ticketing.infrastructure.adapter.web.dto.OrderResponse;
import com.nequi.ticketing.infrastructure.adapter.web.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final PlaceOrderUseCase placeOrder;
    private final GetOrderUseCase getOrder;
    private final ConfirmOrderUseCase confirmOrder;

    public OrderController(PlaceOrderUseCase placeOrder, GetOrderUseCase getOrder, ConfirmOrderUseCase confirmOrder) {
        this.placeOrder = placeOrder;
        this.getOrder = getOrder;
        this.confirmOrder = confirmOrder;
    }

    /** Accepts the purchase and returns immediately; processing happens asynchronously. */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<OrderResponse> place(@RequestHeader(IDEMPOTENCY_HEADER) @NotBlank String idempotencyKey,
                                     @Valid @RequestBody PlaceOrderRequest request) {
        return placeOrder.execute(request.eventId(), request.customerId(), request.quantity(), idempotencyKey)
                .map(OrderResponse::from);
    }

    @GetMapping("/{orderId}")
    public Mono<OrderResponse> get(@PathVariable String orderId) {
        return getOrder.execute(orderId).map(OrderResponse::from);
    }

    @PostMapping("/{orderId}/confirm")
    public Mono<OrderResponse> confirm(@PathVariable String orderId) {
        return confirmOrder.execute(orderId).map(OrderResponse::from);
    }
}
