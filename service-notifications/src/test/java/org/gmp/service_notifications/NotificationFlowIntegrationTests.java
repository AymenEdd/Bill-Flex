package org.gmp.service_notifications;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.gmp.service_notifications.entity.Notification;
import org.gmp.service_notifications.entity.NotificationFailure;
import org.gmp.service_notifications.entity.NotificationStatus;
import org.gmp.service_notifications.entity.NotificationType;
import org.gmp.service_notifications.repository.NotificationRepository;
import org.gmp.service_notifications.service.NotificationDelivery;
import org.gmp.service_notifications.service.NotificationStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.kafka.listener.auto-startup=true",
        "notifications.delivery.scheduling-enabled=true",
        "notifications.delivery.retry-delay=1s",
        "notifications.delivery.scan-delay=200ms",
        "logging.level.org.apache.kafka=WARN"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {
        "invoice-issued", "payment-received", "invoice-issued.DLT", "payment-received.DLT"
}, brokerProperties = {
        "offsets.topic.num.partitions=1",
        "log.index.size.max.bytes=1048576"
}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class NotificationFlowIntegrationTests {

    private static final Map<Long, Integer> CUSTOMER_STATUS = new ConcurrentHashMap<>();
    private static final HttpServer CUSTOMERS = startCustomerFixture();
    private static final SmtpFixture SMTP = new SmtpFixture();
    private static final Duration WAIT = Duration.ofSeconds(20);

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private NotificationStore store;

    @Autowired
    private NotificationDelivery delivery;

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void fixtures(DynamicPropertyRegistry properties) {
        properties.add("notifications.customer.url", () -> "http://127.0.0.1:" + CUSTOMERS.getAddress().getPort());
        properties.add("spring.mail.host", () -> "127.0.0.1");
        properties.add("spring.mail.port", SMTP::port);
    }

    @AfterAll
    static void stopFixtures() throws IOException {
        CUSTOMERS.stop(0);
        SMTP.close();
    }

    @Test
    void consumesInvoiceReplaysAndPaymentWithOneMailPerType() throws Exception {
        publish("invoice-issued", "invoice-1", 101, 11);
        awaitStatus(101, NotificationType.FACTURE_EMISE, NotificationStatus.ENVOYEE);

        publish("invoice-issued", "invoice-1", 101, 11);
        publish("invoice-issued", "invoice-1-redelivery", 101, 11);
        publish("invoice-issued", "invoice-sentinel", 102, 12);
        awaitStatus(102, NotificationType.FACTURE_EMISE, NotificationStatus.ENVOYEE);

        assertThat(repository.findByInvoiceId(101L)).singleElement().satisfies(notification -> {
            assertThat(notification.getRecipientEmail()).isEqualTo(email(11));
            assertThat(notification.getSentAt()).isNotNull();
        });
        assertThat(SMTP.mailCount(email(11))).isEqualTo(1);

        publish("payment-received", "payment-1", 101, 11);
        awaitStatus(101, NotificationType.PAIEMENT_RECU, NotificationStatus.ENVOYEE);
        assertThat(repository.findByInvoiceId(101L)).hasSize(2);
        assertThat(SMTP.mailCount(email(11))).isEqualTo(2);
    }

    @Test
    void retriesUnavailableCustomerWithoutLosingPendingState() throws Exception {
        CUSTOMER_STATUS.put(21L, 503);
        publish("invoice-issued", "invoice-interrupted", 201, 21);

        await().atMost(WAIT).untilAsserted(() -> {
            Notification pending = findNotification(201, NotificationType.FACTURE_EMISE);
            assertThat(pending.getStatus()).isEqualTo(NotificationStatus.EN_ATTENTE);
            assertThat(pending.getLastFailure()).isEqualTo(NotificationFailure.CLIENT_INDISPONIBLE);
            assertThat(pending.getDeliveryStartedAt()).isNull();
            assertThat(SMTP.mailCount(email(21))).isZero();
        });

        CUSTOMER_STATUS.remove(21L);
        awaitStatus(201, NotificationType.FACTURE_EMISE, NotificationStatus.ENVOYEE);
        assertThat(SMTP.mailCount(email(21))).isEqualTo(1);
    }

    @Test
    void distinguishesMissingCustomerFromSmtpRejection() throws Exception {
        CUSTOMER_STATUS.put(31L, 404);
        publish("invoice-issued", "missing-customer", 301, 31);
        awaitStatus(301, NotificationType.FACTURE_EMISE, NotificationStatus.ECHEC);
        assertThat(findNotification(301, NotificationType.FACTURE_EMISE).getLastFailure())
                .isEqualTo(NotificationFailure.CLIENT_INTROUVABLE);
        assertThat(SMTP.mailCount(email(31))).isZero();

        SMTP.reject(email(32));
        publish("invoice-issued", "rejected-smtp", 302, 32);
        awaitStatus(302, NotificationType.FACTURE_EMISE, NotificationStatus.ECHEC);
        Notification failed = findNotification(302, NotificationType.FACTURE_EMISE);
        assertThat(failed.getLastFailure()).isEqualTo(NotificationFailure.ECHEC_SMTP);
        assertThat(failed.getDeliveryStartedAt()).isNotNull();
        assertThat(failed.getSentAt()).isNull();
    }

    @Test
    void invalidPayloadGoesToDeadLetterAndDoesNotCreateNotification() throws Exception {
        String invalid = "{\"eventId\":\"invalid\",\"invoiceId\":401}";
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (KafkaConsumer<String, String> deadLetters = new KafkaConsumer<>(
                config, new StringDeserializer(), new StringDeserializer())) {
            deadLetters.subscribe(List.of("invoice-issued.DLT"));
            kafka.send("invoice-issued", "401", invalid).get(10, TimeUnit.SECONDS);
            ConsumerRecord<String, String> received = KafkaTestUtils.getSingleRecord(
                    deadLetters, "invoice-issued.DLT", WAIT);
            assertThat(received.key()).isEqualTo("401");
            assertThat(received.value()).isEqualTo(invalid);
        }
        assertThat(repository.findByInvoiceId(401L)).isEmpty();
    }

    @Test
    void concurrentDeliveryClaimsPermitOnlyOneSmtpSend() throws Exception {
        Long id = store.create(51L, 501L, NotificationType.FACTURE_EMISE).notification().getId();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = workers.submit(() -> {
                deliverAfter(start, id);
                return null;
            });
            Future<?> second = workers.submit(() -> {
                deliverAfter(start, id);
                return null;
            });
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            awaitStatus(501, NotificationType.FACTURE_EMISE, NotificationStatus.ENVOYEE);
            assertThat(SMTP.mailCount(email(51))).isEqualTo(1);
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void apiCreatesAndFiltersNotificationsAndReportsErrors() throws Exception {
        String body = "{\"clientId\":61,\"invoiceId\":601,\"type\":\"RELANCE\"}";
        mvc.perform(post("/notifications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("RELANCE"));
        awaitStatus(601, NotificationType.RELANCE, NotificationStatus.ENVOYEE);

        mvc.perform(get("/notifications").param("clientId", "61").param("invoiceId", "601"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipientEmail").value(email(61)));
        mvc.perform(get("/notifications").param("invoiceId", "601"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].clientId").value(61));
        mvc.perform(get("/notifications").param("clientId", "61"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].invoiceId").value(601));
        mvc.perform(get("/notifications/{id}", findNotification(601, NotificationType.RELANCE).getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.invoiceId").value(601));

        mvc.perform(post("/notifications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NOTIFICATION_EXISTANTE"));
        mvc.perform(post("/notifications").contentType(MediaType.APPLICATION_JSON).content("{\"clientId\":-1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
        mvc.perform(get("/notifications").param("clientId", "-1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
        mvc.perform(get("/notifications/999999"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTIFICATION_INTROUVABLE"));
        assertThat(SMTP.mailCount(email(61))).isEqualTo(1);
    }

    private void publish(String topic, String eventId, long invoice, long client) throws Exception {
        String event = """
                {"eventId":"%s","invoiceId":%d,"clientId":%d,"additionalUpstreamField":true}
                """.formatted(eventId, invoice, client);
        kafka.send(topic, Long.toString(invoice), event).get(10, TimeUnit.SECONDS);
    }

    private void awaitStatus(long invoice, NotificationType type, NotificationStatus status) {
        await().atMost(WAIT).untilAsserted(() -> assertThat(findNotification(invoice, type).getStatus()).isEqualTo(status));
    }

    private Notification findNotification(long invoice, NotificationType type) {
        var result = repository.findByInvoiceIdAndType(invoice, type);
        assertThat(result).isPresent();
        return result.orElseThrow();
    }

    private void deliverAfter(CountDownLatch start, Long id) throws InterruptedException {
        start.await();
        delivery.deliver(id);
    }

    private static String email(long client) {
        return "client-" + client + "@bill-flex.test";
    }

    private static HttpServer startCustomerFixture() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/clients/", exchange -> {
                try (exchange) {
                    long id = Long.parseLong(exchange.getRequestURI().getPath().substring("/api/clients/".length()));
                    int status = CUSTOMER_STATUS.getOrDefault(id, 200);
                    if (status != 200) {
                        exchange.sendResponseHeaders(status, -1);
                        return;
                    }
                    byte[] json = ("{\"id\":" + id + ",\"email\":\"" + email(id) + "\",\"nom\":\"Fixture\"}")
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, json.length);
                    exchange.getResponseBody().write(json);
                }
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
