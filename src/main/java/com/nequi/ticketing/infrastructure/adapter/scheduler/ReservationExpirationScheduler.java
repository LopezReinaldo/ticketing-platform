package com.nequi.ticketing.infrastructure.adapter.scheduler;

import com.nequi.ticketing.application.usecase.ReleaseExpiredReservationsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationScheduler.class);

    private final ReleaseExpiredReservationsUseCase releaseExpiredReservations;

    public ReservationExpirationScheduler(ReleaseExpiredReservationsUseCase releaseExpiredReservations) {
        this.releaseExpiredReservations = releaseExpiredReservations;
    }

    @Scheduled(fixedDelayString = "${ticketing.reservation.cleanup-interval}")
    public void releaseExpiredReservations() {
        releaseExpiredReservations.execute()
                .doOnNext(count -> {
                    if (count > 0) {
                        log.info("Released {} expired ticket reservations", count);
                    }
                })
                .doOnError(e -> log.error("Expired reservation sweep failed", e))
                .onErrorComplete()
                .block();
    }
}
