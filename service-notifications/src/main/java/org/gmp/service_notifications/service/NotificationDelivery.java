package org.gmp.service_notifications.service;

import java.time.Duration;
import java.time.Instant;

import org.gmp.service_notifications.client.CustomerLookup;
import org.gmp.service_notifications.entity.Notification;
import org.gmp.service_notifications.entity.NotificationFailure;
import org.gmp.service_notifications.entity.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationDelivery {

    private static final Logger log = LoggerFactory.getLogger(NotificationDelivery.class);

    private final NotificationStore store;
    private final CustomerLookup customerLookup;
    private final JavaMailSender mailSender;
    private final String from;
    private final Duration retryDelay;

    public NotificationDelivery(NotificationStore store, CustomerLookup customerLookup,
                                JavaMailSender mailSender,
                                @Value("${notifications.mail.from}") String from,
                                @Value("${notifications.delivery.retry-delay}") Duration retryDelay) {
        this.store = store;
        this.customerLookup = customerLookup;
        this.mailSender = mailSender;
        this.from = from;
        this.retryDelay = retryDelay;
    }

    public void deliver(Long id) {
        Notification notification = store.find(id);
        if (notification.getStatus() != NotificationStatus.EN_ATTENTE
                || notification.getDeliveryStartedAt() != null
                || notification.getNextAttemptAt().isAfter(Instant.now())) {
            return;
        }

        String email;
        try {
            log.info("Recherche du destinataire notificationId={} clientId={}", id, notification.getClientId());
            email = customerLookup.fetchEmail(notification.getClientId());
        } catch (CustomerLookup.Unavailable ex) {
            Instant now = Instant.now();
            if (store.postpone(id, now, now.plus(retryDelay))) {
                log.warn("Clients indisponible, reprise programmée notificationId={}", id);
            }
            return;
        } catch (CustomerLookup.Rejected ex) {
            if (store.reject(id, Instant.now(), ex.getFailure())) {
                log.warn("Destinataire refusé notificationId={} raison={}", id, ex.getFailure());
            }
            return;
        }

        // Commit the claim before talking to SMTP. A replay cannot claim this notification again.
        if (!store.claim(id, email, Instant.now())) {
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(email);
        mail.setSubject(notification.getSubject());
        mail.setText(notification.getMessage());

        try {
            log.info("Tentative SMTP notificationId={} invoiceId={}", id, notification.getInvoiceId());
            mailSender.send(mail);
        } catch (MailException ex) {
            store.finish(id, null, NotificationFailure.ECHEC_SMTP);
            log.warn("Échec SMTP notificationId={} erreur={}", id, ex.getClass().getSimpleName());
            return;
        }
        store.finish(id, Instant.now(), null);
        log.info("Notification envoyée notificationId={} invoiceId={}", id, notification.getInvoiceId());
    }
}
