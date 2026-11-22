package org.gmp.service_notifications;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.gmp.service_notifications.entity.Notification;
import org.gmp.service_notifications.entity.NotificationStatus;
import org.gmp.service_notifications.entity.NotificationType;
import org.gmp.service_notifications.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ServiceNotificationsApplicationTests {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void storesPendingNotificationAndFindsItByBothReferences() {
        Notification saved = repository.saveAndFlush(pending(10L, 20L, NotificationType.FACTURE_EMISE));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findByClientId(10L)).singleElement()
                .extracting(Notification::getStatus).isEqualTo(NotificationStatus.EN_ATTENTE);
        assertThat(repository.findByInvoiceId(20L)).singleElement()
                .extracting(Notification::getId).isEqualTo(saved.getId());
        assertThat(jdbc.queryForObject("select type from notifications where id = ?",
                String.class, saved.getId())).isEqualTo("FACTURE_EMISE");
    }

    @Test
    void allowsOnlyOneConcurrentInsertionPerInvoiceAndType() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = workers.submit(() -> insertAfter(start, 21L));
            Future<Boolean> second = workers.submit(() -> insertAfter(start, 21L));
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS))
                    .isNotEqualTo(second.get(10, TimeUnit.SECONDS));
            assertThat(repository.findByInvoiceId(21L)).hasSize(1);

            repository.saveAndFlush(pending(11L, 21L, NotificationType.PAIEMENT_RECU));
            assertThat(repository.findByInvoiceId(21L)).hasSize(2);
        } finally {
            workers.shutdownNow();
        }
    }

    private boolean insertAfter(CountDownLatch start, long invoiceId) throws InterruptedException {
        start.await();
        try {
            repository.saveAndFlush(pending(11L, invoiceId, NotificationType.FACTURE_EMISE));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    private Notification pending(long clientId, long invoiceId, NotificationType type) {
        return new Notification(clientId, invoiceId, type, "Fixture", "Message", Instant.now());
    }
}
