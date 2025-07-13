package com.amanahfi.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * AmanahFi Event Streaming Application
 * 
 * Provides event streaming infrastructure for Islamic banking operations:
 * - Domain event publishing with Kafka
 * - Islamic banking compliance headers
 * - UAE regulatory compliance tracking
 * - Event sourcing for audit trails
 * - Cross-context communication
 */
@SpringBootApplication
@EnableKafka
public class EventStreamingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventStreamingApplication.class, args);
    }
}