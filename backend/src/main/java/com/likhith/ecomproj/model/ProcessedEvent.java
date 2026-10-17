package com.likhith.ecomproj.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Idempotent Consumer Pattern entity.
 *
 * Tracks which order events have already been processed to prevent duplicate
 * inventory decrements when Kafka redelivers a message (at-least-once delivery).
 *
 * Flow:
 * 1. Consumer receives OrderPlacedEvent.
 * 2. Check: does a ProcessedEvent with this orderId + eventType exist?
 * 3. If yes → skip (already processed, message is a duplicate).
 * 4. If no → process the event, then insert a ProcessedEvent record.
 * 5. The unique constraint on (eventId, eventType) is the final safety net against duplicates.
 */
@Entity
@Table(name = "processed_events", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"eventId", "eventType"})
})
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventId; // UUID or orderId

    @Column(nullable = false)
    private String eventType; // e.g. "ORDER_PLACED"

    private String aggregateId; // e.g. orderId

    @Temporal(TemporalType.TIMESTAMP)
    private Date processedAt;

    @PrePersist
    public void prePersist() {
        if (processedAt == null) processedAt = new Date();
    }
}
