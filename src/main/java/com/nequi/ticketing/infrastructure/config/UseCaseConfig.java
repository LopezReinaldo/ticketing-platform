package com.nequi.ticketing.infrastructure.config;

import com.nequi.ticketing.application.usecase.ConfirmOrderUseCase;
import com.nequi.ticketing.application.usecase.CreateEventUseCase;
import com.nequi.ticketing.application.usecase.GetAvailabilityUseCase;
import com.nequi.ticketing.application.usecase.GetEventUseCase;
import com.nequi.ticketing.application.usecase.GetOrderUseCase;
import com.nequi.ticketing.application.usecase.PlaceOrderUseCase;
import com.nequi.ticketing.application.usecase.ProcessPurchaseUseCase;
import com.nequi.ticketing.application.usecase.ReleaseExpiredReservationsUseCase;
import com.nequi.ticketing.domain.port.EventRepository;
import com.nequi.ticketing.domain.port.OrderRepository;
import com.nequi.ticketing.domain.port.PurchaseQueuePublisher;
import com.nequi.ticketing.domain.port.TicketRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wires framework-agnostic use cases to their infrastructure adapters. */
@Configuration
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateEventUseCase createEventUseCase(EventRepository events, TicketRepository tickets, Clock clock) {
        return new CreateEventUseCase(events, tickets, clock);
    }

    @Bean
    GetEventUseCase getEventUseCase(EventRepository events) {
        return new GetEventUseCase(events);
    }

    @Bean
    GetAvailabilityUseCase getAvailabilityUseCase(EventRepository events, TicketRepository tickets) {
        return new GetAvailabilityUseCase(events, tickets);
    }

    @Bean
    PlaceOrderUseCase placeOrderUseCase(OrderRepository orders, EventRepository events,
                                        PurchaseQueuePublisher publisher, Clock clock) {
        return new PlaceOrderUseCase(orders, events, publisher, clock);
    }

    @Bean
    ProcessPurchaseUseCase processPurchaseUseCase(OrderRepository orders, TicketRepository tickets, Clock clock,
                                                  TicketingProperties props) {
        return new ProcessPurchaseUseCase(orders, tickets, clock, props.reservation().ttl());
    }

    @Bean
    ConfirmOrderUseCase confirmOrderUseCase(OrderRepository orders, TicketRepository tickets, Clock clock) {
        return new ConfirmOrderUseCase(orders, tickets, clock);
    }

    @Bean
    GetOrderUseCase getOrderUseCase(OrderRepository orders) {
        return new GetOrderUseCase(orders);
    }

    @Bean
    ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase(TicketRepository tickets,
                                                                        OrderRepository orders, Clock clock) {
        return new ReleaseExpiredReservationsUseCase(tickets, orders, clock);
    }
}
