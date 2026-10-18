package com.nequi.ticketing.infrastructure.adapter.web;

import com.nequi.ticketing.application.usecase.ConfirmOrderUseCase;
import com.nequi.ticketing.application.usecase.GetOrderUseCase;
import com.nequi.ticketing.application.usecase.PlaceOrderUseCase;
import com.nequi.ticketing.domain.exception.NotFoundException;
import com.nequi.ticketing.domain.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(OrderController.class)
class OrderControllerTest {

    @Autowired WebTestClient client;

    @MockitoBean PlaceOrderUseCase placeOrder;
    @MockitoBean GetOrderUseCase getOrder;
    @MockitoBean ConfirmOrderUseCase confirmOrder;

    private final Order order = Order.create("event-1", "customer-1", 2, "idem-1", Instant.parse("2026-01-01T10:00:00Z"));

    @Test
    void placeOrderReturnsAcceptedWithOrderId() {
        when(placeOrder.execute("event-1", "customer-1", 2, "idem-1")).thenReturn(Mono.just(order));

        client.post().uri("/api/v1/orders")
                .header(OrderController.IDEMPOTENCY_HEADER, "idem-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"eventId":"event-1","customerId":"customer-1","quantity":2}
                        """)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.id").isEqualTo(order.id())
                .jsonPath("$.status").isEqualTo("PENDING");
    }

    @Test
    void placeOrderRequiresIdempotencyKey() {
        client.post().uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"eventId":"event-1","customerId":"customer-1","quantity":2}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void placeOrderValidatesQuantity() {
        client.post().uri("/api/v1/orders")
                .header(OrderController.IDEMPOTENCY_HEADER, "idem-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"eventId":"event-1","customerId":"customer-1","quantity":0}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").value(String.class, detail -> assertThat(detail).contains("quantity"));
    }

    @Test
    void getUnknownOrderReturnsProblemDetail() {
        when(getOrder.execute(anyString())).thenReturn(Mono.error(new NotFoundException("Order", "nope")));

        client.get().uri("/api/v1/orders/nope")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Order with id 'nope' was not found");
    }
}
