package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.exception.ConcurrentModificationException;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.model.OrderStatus;
import com.nequi.ticketing.domain.model.PurchaseRequested;
import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.model.TicketStatus;
import com.nequi.ticketing.domain.port.OrderRepository;
import com.nequi.ticketing.domain.port.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPurchaseUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);

    @Mock OrderRepository orderRepository;
    @Mock TicketRepository ticketRepository;

    private ProcessPurchaseUseCase useCase;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        useCase = new ProcessPurchaseUseCase(orderRepository, ticketRepository, Clock.fixed(NOW, ZoneOffset.UTC), TTL);
        pendingOrder = Order.create("event-1", "customer-1", 2, "idem-1", NOW);
        when(orderRepository.findById(pendingOrder.id())).thenReturn(Mono.just(pendingOrder));
    }

    @Test
    void reservesRequestedTicketsAndMovesOrderToReserved() {
        Ticket t1 = Ticket.available("event-1");
        Ticket t2 = Ticket.available("event-1");
        when(ticketRepository.findAvailableByEvent(eq("event-1"), anyInt())).thenReturn(Flux.just(t1, t2));
        when(ticketRepository.update(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orderRepository.update(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute(PurchaseRequested.from(pendingOrder)))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.RESERVED);
                    assertThat(order.ticketIds()).containsExactly(t1.id(), t2.id());
                    assertThat(order.reservedUntil()).isEqualTo(NOW.plus(TTL));
                })
                .verifyComplete();

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository, org.mockito.Mockito.times(2)).update(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(t -> {
            assertThat(t.status()).isEqualTo(TicketStatus.RESERVED);
            assertThat(t.orderId()).isEqualTo(pendingOrder.id());
        });
    }

    @Test
    void skipsTicketsGrabbedByConcurrentConsumers() {
        Ticket contended = Ticket.available("event-1");
        Ticket t2 = Ticket.available("event-1");
        Ticket t3 = Ticket.available("event-1");
        when(ticketRepository.findAvailableByEvent(eq("event-1"), anyInt())).thenReturn(Flux.just(contended, t2, t3));
        when(ticketRepository.update(any())).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            return t.id().equals(contended.id())
                    ? Mono.error(new ConcurrentModificationException("Ticket", t.id()))
                    : Mono.just(t);
        });
        when(orderRepository.update(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute(PurchaseRequested.from(pendingOrder)))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.RESERVED);
                    assertThat(order.ticketIds()).containsExactly(t2.id(), t3.id());
                })
                .verifyComplete();
    }

    @Test
    void rejectsOrderAndRollsBackWhenInventoryIsInsufficient() {
        Ticket only = Ticket.available("event-1");
        when(ticketRepository.findAvailableByEvent(eq("event-1"), anyInt())).thenReturn(Flux.just(only));
        when(ticketRepository.update(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orderRepository.update(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute(PurchaseRequested.from(pendingOrder)))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
                    assertThat(order.ticketIds()).isEmpty();
                })
                .verifyComplete();

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository, org.mockito.Mockito.times(2)).update(captor.capture());
        assertThat(captor.getAllValues().get(1).status()).isEqualTo(TicketStatus.AVAILABLE);
    }

    @Test
    void ignoresRedeliveredMessageForAlreadyProcessedOrder() {
        Order reserved = pendingOrder.reserved(List.of("t1", "t2"), NOW.plus(TTL), NOW);
        when(orderRepository.findById(pendingOrder.id())).thenReturn(Mono.just(reserved));

        StepVerifier.create(useCase.execute(PurchaseRequested.from(pendingOrder)))
                .expectNext(reserved)
                .verifyComplete();

        verify(ticketRepository, never()).findAvailableByEvent(any(), anyInt());
        verify(orderRepository, never()).update(any());
    }
}
