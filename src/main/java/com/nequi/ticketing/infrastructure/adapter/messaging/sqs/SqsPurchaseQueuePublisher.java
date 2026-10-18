package com.nequi.ticketing.infrastructure.adapter.messaging.sqs;

import com.nequi.ticketing.domain.model.PurchaseRequested;
import com.nequi.ticketing.domain.port.PurchaseQueuePublisher;
import com.nequi.ticketing.infrastructure.config.AwsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Component
public class SqsPurchaseQueuePublisher implements PurchaseQueuePublisher {

    private static final Logger log = LoggerFactory.getLogger(SqsPurchaseQueuePublisher.class);
    private static final Retry RETRY = Retry.backoff(3, Duration.ofMillis(200)).jitter(0.5);

    private final SqsAsyncClient sqs;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public SqsPurchaseQueuePublisher(SqsAsyncClient sqs, ObjectMapper objectMapper, AwsProperties props) {
        this.sqs = sqs;
        this.objectMapper = objectMapper;
        this.queueUrl = props.sqs().purchaseQueueUrl();
    }

    @Override
    public Mono<Void> publish(PurchaseRequested message) {
        return Mono.fromSupplier(() -> objectMapper.writeValueAsString(message))
                .flatMap(body -> Mono.fromFuture(() -> sqs.sendMessage(SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(body)
                        .build())))
                .retryWhen(RETRY)
                .doOnSuccess(r -> log.debug("Published purchase request for order {}", message.orderId()))
                .then();
    }
}
