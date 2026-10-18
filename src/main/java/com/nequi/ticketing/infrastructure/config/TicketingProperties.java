package com.nequi.ticketing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ticketing")
public record TicketingProperties(Reservation reservation) {

    /**
     * @param ttl             how long tickets stay RESERVED without confirmation (max 10 minutes by requirement)
     * @param cleanupInterval delay between runs of the expired-reservation release job
     */
    public record Reservation(Duration ttl, Duration cleanupInterval) {
    }
}
