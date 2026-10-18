package com.nequi.ticketing.infrastructure.adapter.messaging.sqs;

import com.nequi.ticketing.application.usecase.ProcessPurchaseUseCase;
import com.nequi.ticketing.domain.model.PurchaseRequested;
import com.nequi.ticketing.infrastructure.config.AwsProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Long-polling SQS consumer. A message is deleted only after the use case completes successfully;
 * on failure it becomes visible again (at-least-once) and eventually lands in the DLQ via redrive policy.
 */
@Component
public class SqsPurchaseConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsPurchaseConsumer.class);

    private final SqsAsyncClient sqs;
    private final ObjectMapper objectMapper;
    private final ProcessPurchaseUseCase processPurchase;
    private final AwsProperties.Sqs config;
    private Disposable subscription;

    public SqsPurchaseConsumer(SqsAsyncClient sqs, ObjectMapper objectMapper,
                               ProcessPurchaseUseCase processPurchase, AwsProperties props) {
        this.sqs = sqs;
        this.objectMapper = objectMapper;
        this.processPurchase = processPurchase;
        this.config = props.sqs();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        subscription = poll()
                .repeat()
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(s -> log.warn("SQS polling failed, retrying", s.failure())))
                .subscribe();
        log.info("SQS purchase consumer started on {}", config.purchaseQueueUrl());
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
    }

    Flux<Message> poll() {
        var request = ReceiveMessageRequest.builder()
                .queueUrl(config.purchaseQueueUrl())
                .maxNumberOfMessages(config.maxMessages())
                .waitTimeSeconds(config.waitTimeSeconds())
                .build();
        return Mono.fromFuture(() -> sqs.receiveMessage(request))
                .flatMapIterable(r -> r.messages())
                .flatMap(this::handle);
    }

    Mono<Message> handle(Message message) {
        return Mono.fromSupplier(() -> objectMapper.readValue(message.body(), PurchaseRequested.class))
                .flatMap(processPurchase::execute)
                .flatMap(order -> delete(message).thenReturn(message))
                .doOnError(e -> log.error("Failed to process message {}", message.messageId(), e))
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<Void> delete(Message message) {
        return Mono.fromFuture(() -> sqs.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(config.purchaseQueueUrl())
                .receiptHandle(message.receiptHandle())
                .build())).then();
    }
}
