package com.nequi.ticketing.infrastructure.adapter.persistence.dynamodb.entity;

import com.nequi.ticketing.domain.model.Event;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@DynamoDbBean
public class EventEntity {

    private String id;
    private String name;
    private Instant date;
    private String venue;
    private Integer totalCapacity;
    private Instant createdAt;

    public static EventEntity from(Event event) {
        var entity = new EventEntity();
        entity.id = event.id();
        entity.name = event.name();
        entity.date = event.date();
        entity.venue = event.venue();
        entity.totalCapacity = event.totalCapacity();
        entity.createdAt = event.createdAt();
        return entity;
    }

    public Event toDomain() {
        return new Event(id, name, date, venue, totalCapacity, createdAt);
    }

    @DynamoDbPartitionKey
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public Integer getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(Integer totalCapacity) { this.totalCapacity = totalCapacity; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
