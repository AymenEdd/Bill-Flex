package org.gmp.service_notifications.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.gmp.service_notifications.entity.Notification;
import org.gmp.service_notifications.entity.NotificationFailure;
import org.gmp.service_notifications.entity.NotificationStatus;
import org.gmp.service_notifications.entity.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByClientId(Long clientId);

    List<Notification> findByInvoiceId(Long invoiceId);

    List<Notification> findByClientIdAndInvoiceId(Long clientId, Long invoiceId);

    Optional<Notification> findByInvoiceIdAndType(Long invoiceId, NotificationType type);

    @Query("""
            select n.id from Notification n
            where n.status = org.gmp.service_notifications.entity.NotificationStatus.EN_ATTENTE
              and n.deliveryStartedAt is null and n.nextAttemptAt <= :now
            order by n.nextAttemptAt, n.id
            """)
    List<Long> findDueIds(Instant now, Pageable page);

    @Modifying
    @Query("""
            update Notification n
            set n.deliveryStartedAt = :now, n.recipientEmail = :email, n.lastFailure = null
            where n.id = :id
              and n.status = org.gmp.service_notifications.entity.NotificationStatus.EN_ATTENTE
              and n.deliveryStartedAt is null and n.nextAttemptAt <= :now
            """)
    int claimDelivery(Long id, String email, Instant now);

    @Modifying
    @Query("""
            update Notification n
            set n.nextAttemptAt = :nextAttemptAt, n.lastFailure = :failure
            where n.id = :id
              and n.status = org.gmp.service_notifications.entity.NotificationStatus.EN_ATTENTE
              and n.deliveryStartedAt is null and n.nextAttemptAt <= :now
            """)
    int postpone(Long id, Instant now, Instant nextAttemptAt, NotificationFailure failure);

    @Modifying
    @Query("""
            update Notification n
            set n.status = org.gmp.service_notifications.entity.NotificationStatus.ECHEC, n.lastFailure = :failure
            where n.id = :id
              and n.status = org.gmp.service_notifications.entity.NotificationStatus.EN_ATTENTE
              and n.deliveryStartedAt is null and n.nextAttemptAt <= :now
            """)
    int failBeforeDelivery(Long id, Instant now, NotificationFailure failure);

    @Modifying
    @Query("""
            update Notification n
            set n.status = :status, n.lastFailure = :failure, n.sentAt = :sentAt
            where n.id = :id
              and n.status = org.gmp.service_notifications.entity.NotificationStatus.EN_ATTENTE
              and n.deliveryStartedAt is not null
            """)
    int finishDelivery(Long id, NotificationStatus status, NotificationFailure failure, Instant sentAt);
}
