package com.nequi.ticketing.infrastructure.adapter.persistence.dynamodb.entity;

import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.model.TicketStatus;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.time.Instant;

/**
 * Tickets table. Access patterns:
 * <ul>
 *   <li>{@code eventId-status-index}: available tickets of an event / counts per status</li>
 *   <li>{@code orderId-index}: tickets held by an order</li>
 *   <li>{@code status-reservedUntil-index}: expired reservations sweep</li>
 * </ul>
 */
@DynamoDbBean
public class TicketEntity {

    public static final String EVENT_STATUS_INDEX = "eventId-status-index";
    public static final String ORDER_INDEX = "orderId-index";
    public static final String STATUS_RESERVED_UNTIL_INDEX = "status-reservedUntil-index";

    private String id;
    private String eventId;
    private String status;
    private String orderId;
    /** Epoch millis so the GSI sort key orders numerically (ISO strings have variable precision). */
    private Long reservedUntil;
    private Long version;

    public static TicketEntity from(Ticket ticket) {
        var entity = new TicketEntity();
        entity.id = ticket.id();
        entity.eventId = ticket.eventId();
        entity.status = ticket.status().name();
        entity.orderId = ticket.orderId();
        entity.reservedUntil = ticket.reservedUntil() == null ? null : ticket.reservedUntil().toEpochMilli();
        entity.version = ticket.version();
        return entity;
    }

    public Ticket toDomain() {
        Instant until = reservedUntil == null ? null : Instant.ofEpochMilli(reservedUntil);
        return new Ticket(id, eventId, TicketStatus.valueOf(status), orderId, until, version);
    }

    @DynamoDbPartitionKey
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDbSecondaryPartitionKey(indexNames = EVENT_STATUS_INDEX)
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    @DynamoDbSecondarySortKey(indexNames = EVENT_STATUS_INDEX)
    @DynamoDbSecondaryPartitionKey(indexNames = STATUS_RESERVED_UNTIL_INDEX)
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @DynamoDbSecondaryPartitionKey(indexNames = ORDER_INDEX)
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @DynamoDbSecondarySortKey(indexNames = STATUS_RESERVED_UNTIL_INDEX)
    public Long getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(Long reservedUntil) { this.reservedUntil = reservedUntil; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
