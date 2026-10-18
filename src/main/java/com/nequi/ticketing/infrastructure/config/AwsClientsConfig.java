package com.nequi.ticketing.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

@Configuration
public class AwsClientsConfig {

    @Bean
    AwsCredentialsProvider awsCredentialsProvider(AwsProperties props) {
        var credentials = props.credentials();
        if (credentials != null && credentials.accessKey() != null && !credentials.accessKey().isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(credentials.accessKey(), credentials.secretKey()));
        }
        return DefaultCredentialsProvider.builder().build();
    }

    @Bean(destroyMethod = "close")
    DynamoDbAsyncClient dynamoDbAsyncClient(AwsProperties props, AwsCredentialsProvider credentials) {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentials)
                .httpClientBuilder(NettyNioAsyncHttpClient.builder());
        if (hasEndpoint(props)) {
            builder.endpointOverride(props.endpoint());
        }
        return builder.build();
    }

    @Bean
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient client) {
        return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(client).build();
    }

    @Bean(destroyMethod = "close")
    SqsAsyncClient sqsAsyncClient(AwsProperties props, AwsCredentialsProvider credentials) {
        SqsAsyncClientBuilder builder = SqsAsyncClient.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentials)
                .httpClientBuilder(NettyNioAsyncHttpClient.builder());
        if (hasEndpoint(props)) {
            builder.endpointOverride(props.endpoint());
        }
        return builder.build();
    }

    private static boolean hasEndpoint(AwsProperties props) {
        return props.endpoint() != null && !props.endpoint().toString().isBlank();
    }
}
