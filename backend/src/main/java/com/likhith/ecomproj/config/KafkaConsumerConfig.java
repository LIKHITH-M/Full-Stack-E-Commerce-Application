package com.likhith.ecomproj.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka consumer configuration with Dead Letter Topic (DLT) support.
 *
 * When a Kafka message fails processing after 3 retries:
 * - Instead of being lost or blocking the consumer, the message is forwarded
 *   to a ".DLT" topic (e.g., order-events.DLT).
 * - This allows operators to inspect and replay failed messages.
 * - The consumer continues processing subsequent messages without getting stuck.
 */
@Configuration
@Slf4j
public class KafkaConsumerConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(@Autowired(required = false) KafkaOperations<?, ?> kafkaOperations) {
        if (kafkaOperations != null) {
            DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                    (ConsumerRecord<?, ?> record, Exception ex) -> {
                        log.error("Message sent to DLT. Topic: {}, Key: {}, Error: {}",
                                record.topic(), record.key(), ex.getMessage());
                        return new org.apache.kafka.common.TopicPartition(
                                record.topic() + ".DLT", record.partition());
                    });
            return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        }

        // Fallback when Kafka is not configured (e.g., in unit/slice tests)
        return new DefaultErrorHandler(new FixedBackOff(1000L, 3));
    }
}
