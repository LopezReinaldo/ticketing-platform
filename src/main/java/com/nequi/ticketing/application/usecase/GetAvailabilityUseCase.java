package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.exception.NotFoundException;
import com.nequi.ticketing.domain.model.Availability;
import com.nequi.ticketing.domain.model.TicketStatus;
import com.nequi.ticketing.domain.port.EventRepository;
import com.nequi.ticketing.domain.port.TicketRepository;
import reactor.core.publisher.Mono;

/** Real-time availability: {@code available = AVAILABLE}, held tickets include both RESERVED and PENDING_CONFIRMATION. */
public class GetAvailabilityUseCase {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;

    public GetAvailabilityUseCase(EventRepository eventRepository, TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }

    public Mono<Availability> execute(String eventId) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Event", eventId)))
                .flatMap(event -> Mono.zip(
                        count(eventId, TicketStatus.AVAILABLE),
                        count(eventId, TicketStatus.RESERVED),
                        count(eventId, TicketStatus.PENDING_CONFIRMATION),
                        count(eventId, TicketStatus.SOLD)
                ).map(c -> new Availability(eventId, event.totalCapacity(), c.getT1(), c.getT2() + c.getT3(), c.getT4())));
    }

    private Mono<Long> count(String eventId, TicketStatus status) {
        return ticketRepository.countByEventAndStatus(eventId, status).defaultIfEmpty(0L);
    }
}
