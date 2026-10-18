package com.nequi.ticketing.domain.model;

/** Message published to the purchase queue when an order is accepted by the API. */
public record PurchaseRequested(String orderId, String eventId, int quantity) {

    public static PurchaseRequested from(Order order) {
        return new PurchaseRequested(order.id(), order.eventId(), order.quantity());
    }
}
