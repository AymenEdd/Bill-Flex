# Bill-Flex Audit Fix — Design Document

**Goal**: Raise `service-abonnements` from 6.5/11 to 11/11 on the school grading grid by closing 5 gaps.

**Deadline**: Tomorrow (2026-11-23)

---

## 1. Current State vs. Target

| Grid Row | Phase | Current | Target | Gap |
|----------|-------|---------|--------|-----|
| 3 | V1 | 0.5 (CRUD works, no Postman) | 1.0 | **Postman collection** |
| 5 | V2/V3 | 0.5 (Gateway works, no Postman via gateway) | 1.0 | **Postman via gateway** |
| 8 | V4 | 0 (1 Feign client, aggregation exists but only 1 call) | 1.0 | **2nd Feign client + aggregation from multiple services** |
| 9 | V5 | 0 (No broker, no publisher, no async flow) | 1.0 | **Kafka publisher in service-abonnements** |
| 10 | V5 | 0 (No full cycle, no demo evidence) | 1.0 | **Event → broker → consumer → mail + demo** |
| 11 | V6 | 0.5 (ControllerAdvice OK; CB/Retry/Timeout/Idempotency partial) | 1.0 | **@Timeout on Feign, verify CB/Retry on ≥2 calls, idempotency demo** |

---

## 2. Architectural Decisions (Confirmed)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Message broker** | Kafka (already used by service-facturation, service-notifications, service-clients) | Consistency; no new infrastructure |
| **Notification service owner** | Teammate owns `service-notifications` | We publish events; they consume |
| **Business events to publish** | `SubscriptionCreated`, `SubscriptionRenewed`, `SubscriptionExpired`, `SubscriptionCancelled` | Full lifecycle coverage; each demo-able |
| **2nd Feign client for V4** | `service-facturation` — `GET /api/invoices?subscriptionId={id}` | Realistic cross-service aggregation; invoices are the natural complement to subscriptions |
| **Idempotency** | Already implemented (`Idempotency-Key` header + DB unique column) | Just needs demo in Postman |
| **Resilience4j** | Already configured (CB + Retry on 2 methods, Feign timeouts in config) | Add `@Timeout` annotations; verify via Postman chaos test |

---

## 3. New Components to Build

### 3.1 Kafka Publisher in `service-abonnements`
- **Dependency**: `spring-boot-starter-kafka` (add to pom.xml)
- **Config**: Reuse `spring.kafka.bootstrap-servers=localhost:9092` from config-repo
- **Topics** (new, owned by us):
  - `subscription-created`
  - `subscription-renewed`
  - `subscription-expired`
  - `subscription-cancelled`
- **Event DTOs**: Records with `eventId`, `subscriptionId`, `clientId`, `planName`, `dates`, `timestamp`
- **Publisher**: `SubscriptionEventPublisher` component with `KafkaTemplate`
- **Integration points**: Call from `AbonnementService.create()`, `renew()`, `expireSubscriptions()`, `cancel()`

### 3.2 Second Feign Client: `FacturationServiceClient`
- **Interface**: `@FeignClient(name = "service-facturation", url = "${facturation.service.url}")`
- **Method**: `List<InvoiceDto> getInvoicesBySubscription(@PathVariable Long subscriptionId)`
- **DTO**: `InvoiceDto` (id, subscriptionId, clientId, periode, montantHT, montantTTC, echeance, statut)
- **Config**: Add `facturation.service.url` to `service-abonnements.properties` (point to gateway or direct)
- **Gateway route**: Already works via dynamic discovery (`lb://service-facturation`)

### 3.3 Aggregation Endpoint Enhancement
- **Existing**: `GET /api/abonnements/{id}/details` → `AbonnementDetailsResponse` (abonnement + plan + client)
- **Enhanced**: New `GET /api/abonnements/{id}/full-details` → `AbonnementFullDetailsResponse` (above + invoices[])
- **Service method**: `getAbonnementFullDetails(Long id)` calls both Feign clients

### 3.4 Resilience4j `@Timeout` Annotations
- Add `@Timeout(name = "clientById")` and `@Timeout(name = "clientsList")` on `ClientServiceGateway` methods
- Add config entries in `service-abonnements.properties`:
  ```properties
  resilience4j.timelimiter.instances.clientById.timeoutDuration=3s
  resilience4j.timelimiter.instances.clientsList.timeoutDuration=3s
  ```

### 3.5 Postman Collection
Single collection `Bill-Flex-Service-Abonnements.postman_collection.json` with folders:
- **Local Direct** (port 8081): CRUD Abonnement, CRUD Plan, Aggregation, Resilience/Idempotency demos
- **Via Gateway** (port 8089): Same requests routed through `lb://service-abonnements`
- **Tests**: Each request has `pm.test()` assertions (status, schema, idempotency replay, CB trigger)

---

## 4. Demo Evidence Checklist (for V5/V6 rows)

| Evidence | How Captured |
|----------|--------------|
| Kafka message visible | Kafka UI (kafka-ui container) showing `subscription-created` event |
| Email received | Mailpit UI (port 8025) showing expiration reminder / welcome email |
| Circuit Breaker open | Postman request with `Chaos-Mode: fail` header → fallback response |
| Retry triggered | Logs showing 3 attempts before fallback |
| Timeout triggered | Logs showing `TimeoutException` after 3s |
| Idempotency replay | Two POSTs with same `Idempotency-Key` → same response, 2nd returns 200 not 201 |

---

## 5. Files to Modify / Create (Summary)

### `service-abonnements`
| File | Change |
|------|--------|
| `pom.xml` | Add `spring-boot-starter-kafka` |
| `config-repo/service-abonnements.properties` | Add `facturation.service.url`, Kafka bootstrap, resilience4j timeout config |
| `client/FacturationServiceClient.java` | **New** Feign client |
| `client/InvoiceDto.java` | **New** DTO |
| `kafka/SubscriptionEventPublisher.java` | **New** Kafka publisher |
| `kafka/SubscriptionEvent.java` | **New** event records (4 types) |
| `service/AbonnementService.java` | Inject publisher; emit events in create/renew/expire/cancel |
| `service/ClientServiceGateway.java` | Add `@Timeout` annotations |
| `controller/AbonnementController.java` | Add `GET /{id}/full-details` endpoint |
| `dto/AbonnementFullDetailsResponse.java` | **New** composed DTO |
| `dto/InvoiceDto.java` | **New** (or reuse from facturation) |
| `repository/AbonnementRepository.java` | No change (idempotency key already there) |

### `config-repo`
| File | Change |
|------|--------|
| `service-abonnements.properties` | Add `facturation.service.url`, Kafka, resilience4j timeout |

### Root / Docs
| File | Change |
|------|--------|
| `docs/superpowers/plans/bill-flex-audit-fix-plan.md` | Task-by-task plan (this doc's companion) |
| `README.md` | Add: Kafka broker, service-notifications run order, demo steps, Postman import guide |
| `Bill-Flex-Service-Abonnements.postman_collection.json` | **New** structured collection |

### `service-notifications` (teammate — we document only)
| Need | Our Action |
|------|------------|
| Consume `subscription-*` topics | Document expected payload; teammate implements |
| Email templates for subscription events | Document fields available; teammate designs |

---

## 6. Run Order (for README)

```bash
# 1. Infrastructure
docker compose up -d kafka zookeeper kafka-ui mailpit  # (needs docker-compose.yml — see note)

# 2. Platform services
cd config-server && ./mvnw spring-boot:run      # :8888
cd eureka-server && ./mvnw spring-boot:run      # :8761
cd service-gateway && ./mvnw spring-boot:run    # :8089

# 3. Business services (any order)
cd service-clients && ./mvnw spring-boot:run    # :8083
cd service-facturation && ./mvnw spring-boot:run # :8082
cd service-abonnements && ./mvnw spring-boot:run # :8081
cd service-notifications && ./mvnw spring-boot:run # :8085

# 4. Demo
# Import Postman collection → run "Create Subscription" → watch Kafka UI → watch Mailpit
```

> **Note**: No `docker-compose.yml` exists in repo. We'll create a minimal one for Kafka/Zookeeper/Kafka-UI/Mailpit as part of the plan.

---

## 7. Risk Mitigation (Tomorrow Deadline)

| Risk | Mitigation |
|------|------------|
| Timebox overflow | Strict 2h per workstream; if stuck, degrade to "minimum viable" (e.g., 2 events instead of 4) |
| Kafka not starting | Use embedded Kafka for tests; document external broker as manual step |
| service-notifications not consuming our events | Demo shows events IN Kafka UI; teammate consumes later — still proves publisher works |
| Feign client to facturation fails | Gateway discovery already works; fallback to static URL in properties |

---

## 8. Acceptance Criteria (Re-audit = 11/11)

1. **Postman collection** imports and all requests pass (local + gateway)
2. **Aggregation endpoint** `GET /api/abonnements/{id}/full-details` returns subscription + plan + client + invoices[]
3. **Kafka publisher** emits 4 event types; visible in Kafka UI
4. **service-notifications** (teammate) consumes and sends email; visible in Mailpit
5. **Circuit Breaker + Retry + Timeout** on ≥2 Feign calls verified via Postman chaos tests
6. **Idempotency** verified: duplicate `Idempotency-Key` returns original resource
7. **README** covers broker, notification service, run order, demo steps
8. **Re-audit** (read-only) scores 1 on all 11 rows