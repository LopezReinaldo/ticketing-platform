package com.nequi.ticketing.infrastructure.adapter.web;

import com.nequi.ticketing.application.usecase.CreateEventUseCase;
import com.nequi.ticketing.application.usecase.GetAvailabilityUseCase;
import com.nequi.ticketing.application.usecase.GetEventUseCase;
import com.nequi.ticketing.infrastructure.adapter.web.dto.AvailabilityResponse;
import com.nequi.ticketing.infrastructure.adapter.web.dto.CreateEventRequest;
import com.nequi.ticketing.infrastructure.adapter.web.dto.EventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final CreateEventUseCase createEvent;
    private final GetEventUseCase getEvent;
    private final GetAvailabilityUseCase getAvailability;

    public EventController(CreateEventUseCase createEvent, GetEventUseCase getEvent,
                           GetAvailabilityUseCase getAvailability) {
        this.createEvent = createEvent;
        this.getEvent = getEvent;
        this.getAvailability = getAvailability;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return createEvent.execute(request.name(), request.date(), request.venue(), request.totalCapacity())
                .map(EventResponse::from);
    }

    @GetMapping
    public Flux<EventResponse> list() {
        return getEvent.all().map(EventResponse::from);
    }

    @GetMapping("/{eventId}")
    public Mono<EventResponse> get(@PathVariable String eventId) {
        return getEvent.byId(eventId).map(EventResponse::from);
    }

    @GetMapping("/{eventId}/availability")
    public Mono<AvailabilityResponse> availability(@PathVariable String eventId) {
        return getAvailability.execute(eventId).map(AvailabilityResponse::from);
    }
}
