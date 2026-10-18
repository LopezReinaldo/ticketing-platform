package com.nequi.ticketing.infrastructure.adapter.persistence.dynamodb.entity;

import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.model.OrderStatus;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;
import java.util.List;

@DynamoDbBean
public class OrderEntity {

    public static final String IDEMPOTENCY_INDEX = "idempotencyKey-index";

    private String id;
    private String eventId;
    private String customerId;
    private Integer quantity;
    private String idempotencyKey;
    private String status;
    private List<String> ticketIds;
    private Instant reservedUntil;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    public static OrderEntity from(Order order) {
        var entity = new OrderEntity();
        entity.id = order.id();
        entity.eventId = order.eventId();
        entity.customerId = order.customerId();
        entity.quantity = order.quantity();
        entity.idempotencyKey = order.idempotencyKey();
        entity.status = order.status().name();
        entity.ticketIds = order.ticketIds();
        entity.reservedUntil = order.reservedUntil();
        entity.failureReason = order.failureReason();
        entity.createdAt = order.createdAt();
        entity.updatedAt = order.updatedAt();
        entity.version = order.version();
        return entity;
    }

    public Order toDomain() {
        return new Order(id, eventId, customerId, quantity, idempotencyKey, OrderStatus.valueOf(status),
                ticketIds, reservedUntil, failureReason, createdAt, updatedAt, version);
    }

    @DynamoDbPartitionKey
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    @DynamoDbSecondaryPartitionKey(indexNames = IDEMPOTENCY_INDEX)
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getTicketIds() { return ticketIds; }
    public void setTicketIds(List<String> ticketIds) { this.ticketIds = ticketIds; }
    public Instant getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(Instant reservedUntil) { this.reservedUntil = reservedUntil; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
