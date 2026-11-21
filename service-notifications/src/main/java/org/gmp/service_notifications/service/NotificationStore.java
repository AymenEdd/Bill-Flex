package org.gmp.service_notifications.service;

import java.time.Instant;
import java.util.List;

import org.gmp.service_notifications.entity.Notification;
import org.gmp.service_notifications.entity.NotificationFailure;
import org.gmp.service_notifications.entity.NotificationStatus;
import org.gmp.service_notifications.entity.NotificationType;
import org.gmp.service_notifications.exception.NotificationNotFoundException;
import org.gmp.service_notifications.repository.NotificationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationStore {

    private final NotificationRepository repository;

    public NotificationStore(NotificationRepository repository) {
        this.repository = repository;
    }

    public Creation create(Long clientId, Long invoiceId, NotificationType type) {
        Notification existing = repository.findByInvoiceIdAndType(invoiceId, type).orElse(null);
        if (existing != null) {
            return new Creation(existing, false);
        }

        String subject = switch (type) {
            case FACTURE_EMISE -> "Votre facture Bill-Flex n° " + invoiceId;
            case PAIEMENT_RECU -> "Paiement reçu pour la facture n° " + invoiceId;
            case RELANCE -> "Rappel pour la facture Bill-Flex n° " + invoiceId;
        };
        String message = switch (type) {
            case FACTURE_EMISE -> "Votre facture n° " + invoiceId + " a été émise.";
            case PAIEMENT_RECU -> "Nous avons reçu votre paiement pour la facture n° " + invoiceId + ".";
            case RELANCE -> "La facture n° " + invoiceId + " est en attente de paiement.";
        };
        Notification pending = new Notification(clientId, invoiceId, type, subject, message, Instant.now());
        try {
            return new Creation(repository.saveAndFlush(pending), true);
        } catch (DataIntegrityViolationException ex) {
            // The repository insert transaction has rolled back. Only a persisted matching key is a duplicate.
            return repository.findByInvoiceIdAndType(invoiceId, type)
                    .map(winner -> new Creation(winner, false))
                    .orElseThrow(() -> ex);
        }
    }

    public Notification find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
    }

    public List<Notification> list(Long clientId, Long invoiceId) {
        if (clientId != null && invoiceId != null) {
            return repository.findByClientIdAndInvoiceId(clientId, invoiceId);
        }
        if (clientId != null) {
            return repository.findByClientId(clientId);
        }
        if (invoiceId != null) {
            return repository.findByInvoiceId(invoiceId);
        }
        return repository.findAll(Sort.by("id"));
    }

    public List<Long> dueIds(Instant now) {
        return repository.findDueIds(now, PageRequest.ofSize(50));
    }

    @Transactional
    public boolean claim(Long id, String email, Instant now) {
        return repository.claimDelivery(id, email, now) == 1;
    }

    @Transactional
    public boolean postpone(Long id, Instant now, Instant nextAttemptAt) {
        return repository.postpone(id, now, nextAttemptAt, NotificationFailure.CLIENT_INDISPONIBLE) == 1;
    }

    @Transactional
    public boolean reject(Long id, Instant now, NotificationFailure failure) {
        return repository.failBeforeDelivery(id, now, failure) == 1;
    }

    @Transactional
    public void finish(Long id, Instant sentAt, NotificationFailure failure) {
        NotificationStatus status = failure == null ? NotificationStatus.ENVOYEE : NotificationStatus.ECHEC;
        if (repository.finishDelivery(id, status, failure, sentAt) != 1) {
            throw new IllegalStateException("Tentative SMTP introuvable pour la notification " + id);
        }
    }

    public record Creation(Notification notification, boolean created) {
    }
}
