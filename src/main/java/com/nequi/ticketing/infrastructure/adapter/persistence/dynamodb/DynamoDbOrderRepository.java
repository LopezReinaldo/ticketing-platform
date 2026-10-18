package com.nequi.ticketing.infrastructure.adapter.persistence.dynamodb;

import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.port.OrderRepository;
import com.nequi.ticketing.infrastructure.adapter.persistence.dynamodb.entity.OrderEntity;
import com.nequi.ticketing.infrastructure.config.AwsProperties;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
public class DynamoDbOrderRepository implements OrderRepository {

    private final DynamoDbAsyncTable<OrderEntity> table;

    public DynamoDbOrderRepository(DynamoDbEnhancedAsyncClient client, AwsProperties props) {
        this.table = client.table(props.dynamodb().ordersTable(), TableSchema.fromBean(OrderEntity.class));
    }

    @Override
    public Mono<Order> save(Order order) {
        return OptimisticLocking.insert(table, OrderEntity.from(order)).thenReturn(order);
    }

    @Override
    public Mono<Order> update(Order order) {
        return OptimisticLocking.conditionalPut(table, OrderEntity.from(order), order.version(), "Order", order.id())
                .thenReturn(order);
    }

    @Override
    public Mono<Order> findById(String orderId) {
        return Mono.fromFuture(() -> table.getItem(Key.builder().partitionValue(orderId).build()))
                .map(OrderEntity::toDomain);
    }

    @Override
    public Mono<Order> findByIdempotencyKey(String idempotencyKey) {
        var query = QueryConditional.keyEqualTo(Key.builder().partitionValue(idempotencyKey).build());
        return Flux.from(table.index(OrderEntity.IDEMPOTENCY_INDEX).query(query))
                .flatMapIterable(page -> page.items())
                .next()
                .map(OrderEntity::toDomain);
    }
}
