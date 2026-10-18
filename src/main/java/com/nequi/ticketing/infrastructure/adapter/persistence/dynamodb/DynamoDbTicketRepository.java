package com.nequi.ticketing.infrastructure.adapter.persistence.dynamodb;

import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.model.TicketStatus;
import com.nequi.ticketing.domain.port.TicketRepository;
import com.nequi.ticketing.infrastructure.adapter.persistence.dynamodb.entity.TicketEntity;
import com.nequi.ticketing.infrastructure.config.AwsProperties;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.Select;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public class DynamoDbTicketRepository implements TicketRepository {

    /** DynamoDB BatchWriteItem hard limit. */
    private static final int BATCH_SIZE = 25;

    private final DynamoDbEnhancedAsyncClient enhancedClient;
    private final DynamoDbAsyncClient client;
    private final DynamoDbAsyncTable<TicketEntity> table;
    private final String tableName;

    public DynamoDbTicketRepository(DynamoDbEnhancedAsyncClient enhancedClient, DynamoDbAsyncClient client,
                                    AwsProperties props) {
        this.enhancedClient = enhancedClient;
        this.client = client;
        this.tableName = props.dynamodb().ticketsTable();
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(TicketEntity.class));
    }

    @Override
    public Flux<Ticket> saveAll(Flux<Ticket> tickets) {
        return tickets.buffer(BATCH_SIZE).concatMap(this::writeBatch);
    }

    private Flux<Ticket> writeBatch(List<Ticket> batch) {
        var writeBatch = WriteBatch.builder(TicketEntity.class).mappedTableResource(table);
        batch.forEach(ticket -> writeBatch.addPutItem(TicketEntity.from(ticket)));
        var request = BatchWriteItemEnhancedRequest.builder().writeBatches(writeBatch.build()).build();
        return Mono.fromFuture(() -> enhancedClient.batchWriteItem(request))
                .flatMapMany(result -> {
                    List<Ticket> unprocessed = result.unprocessedPutItemsForTable(table).stream()
                            .map(TicketEntity::toDomain).toList();
                    Flux<Ticket> written = Flux.fromIterable(batch).filter(t -> !unprocessed.contains(t));
                    return unprocessed.isEmpty() ? written : written.concatWith(writeBatch(unprocessed));
                });
    }

    @Override
    public Mono<Ticket> update(Ticket ticket) {
        return OptimisticLocking.conditionalPut(table, TicketEntity.from(ticket), ticket.version(), "Ticket", ticket.id())
                .thenReturn(ticket);
    }

    @Override
    public Flux<Ticket> findAvailableByEvent(String eventId, int limit) {
        var key = Key.builder().partitionValue(eventId).sortValue(TicketStatus.AVAILABLE.name()).build();
        var request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(key))
                .limit(limit)
                .build();
        return query(TicketEntity.EVENT_STATUS_INDEX, request).take(limit);
    }

    @Override
    public Flux<Ticket> findByOrderId(String orderId) {
        var request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(orderId).build()))
                .build();
        return query(TicketEntity.ORDER_INDEX, request);
    }

    @Override
    public Flux<Ticket> findExpiredReservations(Instant now) {
        var key = Key.builder().partitionValue(TicketStatus.RESERVED.name()).sortValue(now.toEpochMilli()).build();
        var request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.sortLessThanOrEqualTo(key))
                .build();
        return query(TicketEntity.STATUS_RESERVED_UNTIL_INDEX, request);
    }

    @Override
    public Mono<Long> countByEventAndStatus(String eventId, TicketStatus status) {
        var request = QueryRequest.builder()
                .tableName(tableName)
                .indexName(TicketEntity.EVENT_STATUS_INDEX)
                .keyConditionExpression("eventId = :eventId AND #s = :status")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of(
                        ":eventId", AttributeValue.fromS(eventId),
                        ":status", AttributeValue.fromS(status.name())))
                .select(Select.COUNT)
                .build();
        return Flux.from(client.queryPaginator(request))
                .map(QueryResponse::count)
                .reduce(0L, (acc, count) -> acc + count);
    }

    private Flux<Ticket> query(String indexName, QueryEnhancedRequest request) {
        return Flux.from(table.index(indexName).query(request))
                .flatMapIterable(Page::items)
                .map(TicketEntity::toDomain);
    }
}
