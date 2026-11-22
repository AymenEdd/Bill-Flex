package org.gmp.service_notifications.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notifications.delivery.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class PendingNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(PendingNotificationJob.class);

    private final NotificationStore store;
    private final NotificationDelivery delivery;

    public PendingNotificationJob(NotificationStore store, NotificationDelivery delivery) {
        this.store = store;
        this.delivery = delivery;
    }

    @Scheduled(fixedDelayString = "${notifications.delivery.scan-delay}")
    public void retryDue() {
        for (Long id : store.dueIds(Instant.now())) {
            try {
                delivery.deliver(id);
            } catch (RuntimeException ex) {
                log.error("Traitement interrompu notificationId={}", id, ex);
            }
        }
    }
}
