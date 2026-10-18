package com.nequi.ticketing.infrastructure.adapter.persistence.dynamodb;

import com.nequi.ticketing.domain.exception.ConcurrentModificationException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Conditional writes on a numeric {@code version} attribute. A write only succeeds when the stored
 * version equals {@code newVersion - 1}; otherwise a {@link ConcurrentModificationException} is emitted.
 */
final class OptimisticLocking {

    private OptimisticLocking() {
    }

    static <T> Mono<T> conditionalPut(DynamoDbAsyncTable<T> table, T item, long newVersion,
                                      String resource, String id) {
        Expression condition = Expression.builder()
                .expression("#v = :expected")
                .putExpressionName("#v", "version")
                .putExpressionValue(":expected", AttributeValue.fromN(Long.toString(newVersion - 1)))
                .build();
        PutItemEnhancedRequest<T> request = PutItemEnhancedRequest.builder(table.tableSchema().itemType().rawClass())
                .item(item)
                .conditionExpression(condition)
                .build();
        return Mono.fromFuture(() -> table.putItem(request))
                .onErrorMap(ConditionalCheckFailedException.class,
                        e -> new ConcurrentModificationException(resource, id))
                .thenReturn(item);
    }

    static <T> Mono<T> insert(DynamoDbAsyncTable<T> table, T item) {
        Expression notExists = Expression.builder().expression("attribute_not_exists(id)").build();
        PutItemEnhancedRequest<T> request = PutItemEnhancedRequest.builder(table.tableSchema().itemType().rawClass())
                .item(item)
                .conditionExpression(notExists)
                .build();
        return Mono.fromFuture(() -> table.putItem(request)).thenReturn(item);
    }
}
