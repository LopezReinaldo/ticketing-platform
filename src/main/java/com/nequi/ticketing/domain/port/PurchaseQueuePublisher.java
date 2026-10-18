package com.nequi.ticketing.domain.port;

import com.nequi.ticketing.domain.model.PurchaseRequested;
import reactor.core.publisher.Mono;

/** Outbound port to the asynchronous purchase-processing queue (SQS in production). */
public interface PurchaseQueuePublisher {

    Mono<Void> publish(PurchaseRequested message);
}
