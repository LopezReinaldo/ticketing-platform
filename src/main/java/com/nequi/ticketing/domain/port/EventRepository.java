package com.nequi.ticketing.domain.port;

import com.nequi.ticketing.domain.model.Event;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventRepository {

    Mono<Event> save(Event event);

    Mono<Event> findById(String eventId);

    Flux<Event> findAll();
}
