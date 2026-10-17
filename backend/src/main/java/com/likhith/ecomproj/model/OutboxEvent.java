package com.likhith.ecomproj.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Transactional Outbox Pattern entity.
 *
 * Instead of publishing directly to Kafka during checkout (which creates a dual-write problem
 * where DB commit succeeds but Kafka publish fails, losing the event), events are saved to
 * this outbox table in the SAME database transaction as the order.
 *
 * A scheduled OutboxPublisher then drains pending events to Kafka. If Kafka is down,
 * events remain safely in the outbox and are retried later — zero event loss.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status", columnList = "status"),
    @Index(name = "idx_outbox_created_at", columnList = "createdAt")
})
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String aggregateId; // e.g. orderId

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload; // JSON serialized event

    @Column(nullable = false)
    private String status; // PENDING, PUBLISHED, FAILED

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date processedAt;

    private int retryCount;

    @PrePersist
    public void prePersist() {
        if (eventId == null) eventId = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = new Date();
        if (status == null) status = "PENDING";
    }
}
