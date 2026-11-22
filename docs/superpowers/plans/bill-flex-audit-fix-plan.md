# Bill-Flex Audit Fix — Execution Plan

**Target**: 11/11 on grading grid by **tomorrow (2026-11-23)**  
**Design**: [`../specs/bill-flex-audit-fix-design.md`](../specs/bill-flex-audit-fix-design.md)

---

## Workstream Overview (Sequential for Timeboxing)

| Workstream | Focus | Grid Rows | Est. Time | Demo Artifact |
|------------|-------|-----------|-----------|---------------|
| **A** | Postman + V4 Aggregation | 3, 5, 8 | ~3h | Postman collection + `GET /full-details` |
| **B** | Resilience4j Polish + Idempotency Demo | 11 | ~2h | Chaos tests in Postman |
| **C** | Kafka Publisher + V5 Demo | 9, 10 | ~3h | Events in Kafka UI + Mailpit email |
| **D** | Docker Compose + README + Re-audit | All | ~1h | `docker-compose.yml`, updated README, 11/11 score |

**Total**: ~9h — tight but doable with focus.

---

## Workstream A — Postman + V4 Aggregation

### A1. Add `facturation.service.url` to config
- **File**: `config-repo/service-abonnements.properties`
- **Add**:
  ```properties
  facturation.service.url=http://localhost:8082
  ```
- **Check**: Config Server serves it (curl `localhost:8888/service-abonnements/default`)

### A2. Create `InvoiceDto` (mirror of facturation's)
- **File**: `service-abonnements/src/main/java/org/gmp/serviceabonnements/client/InvoiceDto.java`
- **Fields**: `id`, `subscriptionId`, `clientId`, `periode`, `montantHT`, `montantTTC`, `echeance`, `statut`
- **Annotations**: `@JsonIgnoreProperties(ignoreUnknown = true)`, record or Lombok `@Data`

### A3. Create `FacturationServiceClient` Feign interface
- **File**: `service-abonnements/src/main/java/org/gmp/serviceabonnements/client/FacturationServiceClient.java`
- **Code**:
  ```java
  @FeignClient(name = "service-facturation", url = "${facturation.service.url}")
  public interface FacturationServiceClient {
      @GetMapping("/api/invoices")
      List<InvoiceDto> getInvoicesBySubscription(@RequestParam("subscriptionId") Long subscriptionId);
  }
  ```
- **Check**: Compiles; no runtime yet

### A4. Create `AbonnementFullDetailsResponse` DTO
- **File**: `service-abonnements/src/main/java/org/gmp/serviceabonnements/dto/AbonnementFullDetailsResponse.java`
- **Composition**: `AbonnementResponse abonnement`, `PlanDetailsResponse plan`, `ClientDto client`, `List<InvoiceDto> invoices`
- **Pattern**: Follow `AbonnementDetailsResponse` style

### A5. Add aggregation service method
- **File**: `service-abonnements/src/main/java/org/gmp/serviceabonnements/service/AbonnementService.java`
- **Method**:
  ```java
  @Transactional(readOnly = true)
  public AbonnementFullDetailsResponse getAbonnementFullDetails(Long id) {
      AbonnementDetailsResponse base = getAbonnementDetails(id); // reuses existing logic
      List<InvoiceDto> invoices = facturationServiceClient.getInvoicesBySubscription(id);
      return new AbonnementFullDetailsResponse(base.abonnement(), base.planDetails(), base.client(), invoices);
  }
  ```
- **Inject**: `FacturationServiceClient` in constructor

### A6. Add controller endpoint
- **File**: `service-abonnements/src/main/java/org/gmp/serviceabonnements/controller/AbonnementController.java`
- **Endpoint**:
  ```java
  @GetMapping("/{id}/full-details")
  public AbonnementFullDetailsResponse getAbonnementFullDetails(@PathVariable Long id) {
      return abonnementService.getAbonnementFullDetails(id);
  }
  ```

### A7. Build Postman Collection (`Bill-Flex-Service-Abonnements.postman_collection.json`)
**Structure**:
```
Bill-Flex Service Abonnements
├── Local Direct (port 8081)
│   ├── Plans: Create, Get, Update, Deactivate, List
│   ├── Abonnements: Create (with Idempotency-Key), Get, Update, Suspend, Reactivate, Cancel, Renew, Search, Quota, Statistics, History, Upcoming Expirations, Send Notifications
│   ├── Aggregation: Get Details (1 Feign), Get Full Details (2 Feign)
│   ├── Resilience: Trigger CB (Chaos header), Trigger Timeout, Trigger Retry
│   └── Idempotency: Duplicate Create → assert same ID
├── Via Gateway (port 8089)
│   └── (mirror of Local Direct with {{gatewayUrl}} variable)
└── Test Scripts: pm.test() on every request (status, JSON schema, idempotency replay)
```
- **Variables**: `baseUrl` (8081), `gatewayUrl` (8089), `clientId` (pre-seeded), `planId` (pre-seeded)
- **Pre-request scripts**: Generate UUID for `Idempotency-Key` on create
- **Save**: Root of repo

### A8. Verify Workstream A
- **Command**: `cd service-abonnements && ./mvnw compile`
- **Test**: Start service-abonnements + service-facturation + gateway → run Postman collection "Local Direct" folder → all green
- **Evidence**: Postman run screenshot / export

---

## Workstream B — Resilience4j Polish + Idempotency Demo

### B1. Add `@Timeout` annotations to `ClientServiceGateway`
- **File**: `service-abonnements/src/main/java/org/gmp/serviceabonnements/client/ClientServiceGateway.java`
- **Change**:
  ```java
  @CircuitBreaker(name = "clientById", fallbackMethod = "getClientByIdFallback")
  @Retry(name = "clientById")
  @Timeout(name = "clientById")
  public ClientDto getClientById(Long id) { ... }

  @CircuitBreaker(name = "clientsList", fallbackMethod = "getAllClientsFallback")
  @Retry(name = "clientsList")
  @Timeout(name = "clientsList")
  public List<ClientDto> getAllClients() { ... }
  ```

### B2. Add Timeout config to properties
- **File**: `config-repo/service-abonnements.properties`
- **Add**:
  ```properties
  # RESILIENCE4J - TIMEOUT (TimeLimiter)
  resilience4j.timelimiter.instances.clientById.timeoutDuration=3s
  resilience4j.timelimiter.instances.clientById.cancelRunningFuture=true
  resilience4j.timelimiter.instances.clientsList.timeoutDuration=3s
  resilience4j.timelimiter.instances.clientsList.cancelRunningFuture=true
  ```

### B3. Add chaos-test endpoints (optional, for demo)
- **File**: `service-abonnements/src/main/java/org/gmp/serviceabonnements/controller/AbonnementController.java`
- **Endpoints** (dev profile only):
  ```java
  @GetMapping("/chaos/client/{id}/fail")
  public ClientDto triggerClientFailure(@PathVariable Long id, @RequestHeader("Chaos-Mode") String mode) {
      if ("fail".equals(mode)) throw new RuntimeException("Chaos: forced failure");
      return clientServiceGateway.getClientById(id);
  }
  ```
- **Or**: Use `service-clients` side to simulate failure (preferred — no code pollution)

### B4. Add Postman Chaos Tests
- **Collection**: Add requests under "Resilience" folder:
  - `GET /api/abonnements/chaos/client/999/fail` with header `Chaos-Mode: fail` → expect fallback response (503)
  - `GET /api/abonnements/clients` with network delay (tc/chaos-mesh) → verify retry logs
  - `GET /api/abonnements/{id}/details` with timeout → verify `@Timeout` fallback

### B5. Verify Idempotency in Postman
- **Test**: `POST /api/abonnements` with same `Idempotency-Key` twice
- **Assert**: 2nd response status 200 (not 201), same `id` in body

### B6. Verify Workstream B
- **Command**: `cd service-abonnements && ./mvnw compile`
- **Test**: Run Postman "Resilience" + "Idempotency" folders → all green
- **Logs**: Check `CircuitBreaker`, `Retry`, `Timeout` events in console

---

## Workstream C — Kafka Publisher + V5 Demo

### C1. Add Kafka dependency
- **File**: `service-abonnements/pom.xml`
- **Add** in `<dependencies>`:
  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-kafka</artifactId>
  </dependency>
  ```

### C2. Add Kafka config to properties
- **File**: `config-repo/service-abonnements.properties`
- **Add**:
  ```properties
  # KAFKA
  spring.kafka.bootstrap-servers=localhost:9092
  spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
  spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
  ```

### C3. Create Subscription Event Records
- **File**: `service-abonnements/src/main/java/org/gmp/serviceabonnements/kafka/SubscriptionEvent.java`
- **Events** (4 records, shared `eventId`, `timestamp`, `subscriptionId`, `clientId`, `planName`):
  ```java
  public sealed interface SubscriptionEvent permits SubscriptionCreatedEvent, SubscriptionRenewedEvent, SubscriptionExpiredEvent, SubscriptionCancelledEvent {
      String eventId(); Instant timestamp(); Long subscriptionId(); Long clientId(); String planName();
  }
  public record SubscriptionCreatedEvent(...) implements SubscriptionEvent {}
  public record SubscriptionRenewedEvent(...) implements SubscriptionEvent {}
  public record SubscriptionExpiredEvent(...) implements SubscriptionEvent {}
  public record SubscriptionCancelledEvent(...) implements SubscriptionEvent {}
  ```

### C4. Create `SubscriptionEventPublisher`
- **File**: `service-abonnements/src/main/java/org/gmp/serviceabonnements/kafka/SubscriptionEventPublisher.java`
- **Code**:
  ```java
  @Component @RequiredArgsConstructor
  public class SubscriptionEventPublisher {
      private final KafkaTemplate<String, SubscriptionEvent> kafkaTemplate;
      public void publishCreated(Abonnement a, String planName) { send("subscription-created", new SubscriptionCreatedEvent(...)); }
      public void publishRenewed(Abonnement a, String planName) { send("subscription-renewed", new SubscriptionRenewedEvent(...)); }
      public void publishExpired(Abonnement a, String planName) { send("subscription-expired", new SubscriptionExpiredEvent(...)); }
      public void publishCancelled(Abonnement a, String planName) { send("subscription-cancelled", new SubscriptionCancelledEvent(...)); }
      private void send(String topic, SubscriptionEvent event) {
          kafkaTemplate.send(topic, String.valueOf(event.subscriptionId()), event)
              .whenComplete((r, ex) -> { if (ex != null) log.error("Kafka send failed", ex); else log.info("Sent {} to {}", event.getClass().getSimpleName(), topic); });
      }
  }
  ```

### C5. Wire Publisher into `AbonnementService`
- **File**: `service-abonnements/src/main/java/org/gmp/serviceabonnements/service/AbonnementService.java`
- **Constructor**: Add `SubscriptionEventPublisher publisher`
- **Calls**:
  - `create()` → `publisher.publishCreated(saved, plan.getNom())`
  - `renew()` → `publisher.publishRenewed(saved, plan.getNom())`
  - `expireSubscriptions()` → for each expired: `publisher.publishExpired(ab, plan.getNom())`
  - `cancel()` → `publisher.publishCancelled(ab, plan.getNom())`

### C6. Create Minimal `docker-compose.yml` (Root)
- **File**: `docker-compose.yml` (root)
- **Services**: `zookeeper`, `kafka`, `kafka-ui` (port 8080), `mailpit` (ports 1025, 8025)
- **Image versions**: Confluent 7.5, provectuslabs/kafka-ui, axllent/mailpit
- **Network**: `bill-flex-net`

### C7. Verify Workstream C
- **Command**: `docker compose up -d` → wait for Kafka UI at `localhost:8080`
- **Test**: Run Postman "Create Subscription" → check Kafka UI → topic `subscription-created` has message
- **Teammate**: Ask them to run `service-notifications` and verify Mailpit receives email
- **Fallback**: If teammate unavailable, screenshot Kafka UI message payload as evidence

---

## Workstream D — Docker Compose + README + Re-audit

### D1. Finalize `docker-compose.yml`
- Ensure all ports match README table
- Add healthchecks for Kafka, Mailpit
- Document in README

### D2. Update Root `README.md`
**Add Sections**:
1. **Message Broker** — Kafka topics, event schemas, run `docker compose up`
2. **Notification Service** — Owned by teammate; consumes `subscription-*` + `invoice-*` topics; sends email via Mailpit
3. **Run Order** — Numbered steps (infra → platform → business)
4. **Demo Steps** — Import Postman → run "Create Subscription" → watch Kafka UI → watch Mailpit
5. **Postman Guide** — Import collection, set variables, run folders
6. **Resilience Demo** — How to trigger CB/Retry/Timeout via chaos headers

### D3. Re-audit (Read-Only)
- **Script**: Re-run the 11-row grid against current code
- **Command**: Manual verification per row (no code execution needed)
- **Output**: Update `AUDIT_REPORT.md` with new scores (target: all 1.0)

### D4. Final Commit
- `git add` all new/modified files
- `git commit -m "chore: audit fix — Postman, aggregation, resilience, Kafka publisher, docs"`
- **No push** unless user asks

---

## Task Checklist (Copy-Paste for Tracking)

```
[ ] A1  config-repo/service-abonnements.properties + facturation.service.url
[ ] A2  client/InvoiceDto.java
[ ] A3  client/FacturationServiceClient.java
[ ] A4  dto/AbonnementFullDetailsResponse.java
[ ] A5  service/AbonnementService.getAbonnementFullDetails()
[ ] A6  controller/AbonnementController.getAbonnementFullDetails()
[ ] A7  Bill-Flex-Service-Abonnements.postman_collection.json
[ ] A8  Verify: compile + Postman Local Direct green

[ ] B1  ClientServiceGateway + @Timeout
[ ] B2  config-repo/service-abonnements.properties + timelimiter config
[ ] B3  (Optional) Chaos test endpoint or use service-clients side
[ ] B4  Postman Resilience folder tests
[ ] B5  Postman Idempotency test
[ ] B6  Verify: compile + Postman Resilience/Idempotency green

[ ] C1  pom.xml + spring-boot-starter-kafka
[ ] C2  config-repo/service-abonnements.properties + Kafka config
[ ] C3  kafka/SubscriptionEvent.java (4 records)
[ ] C4  kafka/SubscriptionEventPublisher.java
[ ] C5  AbonnementService + publisher wiring
[ ] C6  docker-compose.yml (root)
[ ] C7  Verify: docker compose up + Postman Create → Kafka UI message

[ ] D1  docker-compose.yml finalize
[ ] D2  README.md update (broker, notif, run order, demo, Postman, resilience)
[ ] D3  Re-audit → all 11 rows = 1.0
[ ] D4  git commit
```

---

## Timeboxing Rules (Critical for Tomorrow)

| Rule | Enforcement |
|------|-------------|
| Max 2h per workstream | Timer; if exceeded, scope down (e.g., 2 events instead of 4) |
| No gold-plating | Only what the grid asks for (YAGNI) |
| Demo-first | Every task ends with a checkable artifact (Postman green, Kafka UI message, Mailpit email) |
| No side quests | Don't fix unrelated bugs, don't refactor unrelated code |
| Commit early | After each workstream verification |