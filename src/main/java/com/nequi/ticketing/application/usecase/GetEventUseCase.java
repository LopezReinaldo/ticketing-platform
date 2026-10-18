package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.exception.NotFoundException;
import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.port.EventRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GetEventUseCase {

    private final EventRepository eventRepository;

    public GetEventUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Mono<Event> byId(String eventId) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Event", eventId)));
    }

    public Flux<Event> all() {
        return eventRepository.findAll();
    }
}
