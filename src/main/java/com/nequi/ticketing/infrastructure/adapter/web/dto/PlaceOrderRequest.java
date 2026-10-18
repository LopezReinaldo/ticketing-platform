package com.nequi.ticketing.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PlaceOrderRequest(
        @NotBlank String eventId,
        @NotBlank String customerId,
        @Min(1) @Max(10) int quantity
) {
}
