package com.nequi.ticketing.infrastructure.adapter.persistence.dynamodb;

import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.port.EventRepository;
import com.nequi.ticketing.infrastructure.adapter.persistence.dynamodb.entity.EventEntity;
import com.nequi.ticketing.infrastructure.config.AwsProperties;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class DynamoDbEventRepository implements EventRepository {

    private final DynamoDbAsyncTable<EventEntity> table;

    public DynamoDbEventRepository(DynamoDbEnhancedAsyncClient client, AwsProperties props) {
        this.table = client.table(props.dynamodb().eventsTable(), TableSchema.fromBean(EventEntity.class));
    }

    @Override
    public Mono<Event> save(Event event) {
        return OptimisticLocking.insert(table, EventEntity.from(event)).thenReturn(event);
    }

    @Override
    public Mono<Event> findById(String eventId) {
        return Mono.fromFuture(() -> table.getItem(Key.builder().partitionValue(eventId).build()))
                .map(EventEntity::toDomain);
    }

    @Override
    public Flux<Event> findAll() {
        return Flux.from(table.scan().items()).map(EventEntity::toDomain);
    }
}
