package com.nequi.ticketing.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateEventRequest(
        @NotBlank String name,
        @NotNull @Future Instant date,
        @NotBlank String venue,
        @Min(1) @Max(100_000) int totalCapacity
) {
}
