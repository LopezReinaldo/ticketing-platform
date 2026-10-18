package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.exception.NotFoundException;
import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.model.OrderStatus;
import com.nequi.ticketing.domain.model.PurchaseRequested;
import com.nequi.ticketing.domain.port.EventRepository;
import com.nequi.ticketing.domain.port.OrderRepository;
import com.nequi.ticketing.domain.port.PurchaseQueuePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceOrderUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Mock OrderRepository orderRepository;
    @Mock EventRepository eventRepository;
    @Mock PurchaseQueuePublisher publisher;

    private PlaceOrderUseCase useCase;
    private final Event event = Event.create("Show", NOW.plusSeconds(3600), "Arena", 10, NOW);

    @BeforeEach
    void setUp() {
        useCase = new PlaceOrderUseCase(orderRepository, eventRepository, publisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsPendingOrderAndPublishesMessage() {
        when(orderRepository.findByIdempotencyKey("idem-1")).thenReturn(Mono.empty());
        when(eventRepository.findById(event.id())).thenReturn(Mono.just(event));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publisher.publish(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(event.id(), "customer-1", 2, "idem-1"))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
                    assertThat(order.quantity()).isEqualTo(2);
                    assertThat(order.idempotencyKey()).isEqualTo("idem-1");
                    verify(publisher).publish(PurchaseRequested.from(order));
                })
                .verifyComplete();
    }

    @Test
    void returnsExistingOrderForRepeatedIdempotencyKey() {
        Order existing = Order.create(event.id(), "customer-1", 2, "idem-1", NOW);
        when(orderRepository.findByIdempotencyKey("idem-1")).thenReturn(Mono.just(existing));

        StepVerifier.create(useCase.execute(event.id(), "customer-1", 2, "idem-1"))
                .expectNext(existing)
                .verifyComplete();

        verify(orderRepository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void failsWhenEventDoesNotExist() {
        when(orderRepository.findByIdempotencyKey("idem-1")).thenReturn(Mono.empty());
        when(eventRepository.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("missing", "customer-1", 2, "idem-1"))
                .expectError(NotFoundException.class)
                .verify();

        verify(publisher, never()).publish(any());
    }
}
