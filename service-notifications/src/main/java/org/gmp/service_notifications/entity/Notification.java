package org.gmp.service_notifications.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_invoice_type", columnNames = {"invoice_id", "type"}))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false)
    private Long invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationType type;

    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationStatus status = NotificationStatus.EN_ATTENTE;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    private Instant deliveryStartedAt;
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private NotificationFailure lastFailure;

    protected Notification() {
    }

    public Notification(Long clientId, Long invoiceId, NotificationType type,
                        String subject, String message, Instant createdAt) {
        this.clientId = clientId;
        this.invoiceId = invoiceId;
        this.type = type;
        this.subject = subject;
        this.message = message;
        this.createdAt = createdAt;
        this.nextAttemptAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getClientId() {
        return clientId;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getDeliveryStartedAt() {
        return deliveryStartedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public NotificationFailure getLastFailure() {
        return lastFailure;
    }
}
