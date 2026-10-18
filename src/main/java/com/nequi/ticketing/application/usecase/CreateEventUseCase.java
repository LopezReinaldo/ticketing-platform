package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.port.EventRepository;
import com.nequi.ticketing.domain.port.TicketRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;

/** Creates an event and materializes its full ticket inventory as {@code AVAILABLE} tickets. */
public class CreateEventUseCase {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final Clock clock;

    public CreateEventUseCase(EventRepository eventRepository, TicketRepository ticketRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.clock = clock;
    }

    public Mono<Event> execute(String name, Instant date, String venue, int totalCapacity) {
        return Mono.fromSupplier(() -> Event.create(name, date, venue, totalCapacity, clock.instant()))
                .flatMap(eventRepository::save)
                .flatMap(event -> ticketRepository
                        .saveAll(Flux.range(0, event.totalCapacity()).map(i -> Ticket.available(event.id())))
                        .then(Mono.just(event)));
    }
}
