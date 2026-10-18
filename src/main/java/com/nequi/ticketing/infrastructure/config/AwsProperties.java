package com.nequi.ticketing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * AWS connectivity. {@code endpoint} is only set for local development (LocalStack / DynamoDB Local);
 * in AWS the default credential chain and regional endpoints are used.
 */
@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
        String region,
        URI endpoint,
        Credentials credentials,
        DynamoDb dynamodb,
        Sqs sqs
) {

    public record Credentials(String accessKey, String secretKey) {
    }

    public record DynamoDb(String eventsTable, String ticketsTable, String ordersTable) {
    }

    public record Sqs(String purchaseQueueUrl, int maxMessages, int waitTimeSeconds) {
    }
}
