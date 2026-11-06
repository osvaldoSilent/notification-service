# Role & Purpose
You are the **Context Synthesizer** for `notifications-service` (`com.osvaldevops.notifications`).
Your purpose is to provide an immediate, high-density architectural snapshot of the project so any AI agent session starts with complete context without needing to scan the entire repository, and to update the **Iteration Log** after every major milestone.

---

# 1. Project Snapshot (`notifications-service`)
- **Stack:** Java 25, Spring Boot 3.4.5, Maven, Hexagonal Architecture (100% pure Java `domain`).
- **Messaging (Inbound):** Google Cloud Pub/Sub (`spring-cloud-gcp-starter-pubsub` `5.9.0`) supporting both:
  1. **Push HTTP Webhook (`POST /api/v1/pubsub/events`)** via `PubSubPushConsumerAdapter` for serverless scale-to-zero on Cloud Run.
  2. **Pull Subscription Listener (`PubSubUserCreatedSubscriberAdapter`)** subscribing to `${app.pubsub.subscriptions.user-created}` when `app.pubsub.subscriber-enabled=true`.
- **Email Delivery (Outbound):** Spring Boot Starter Mail (`JavaMailSender` via SMTP / Resend) implemented in `EmailNotificationSenderAdapter` with simulation mode fallback (`app.notifications.mail-enabled=false`).
- **Quality & CI/CD:** JaCoCo `0.8.13` (100% unit test coverage across 8 analyzed classes / 32 unit tests), Cucumber `7.15.0`, SonarCloud, Jib (`eclipse-temurin:25-jre-alpine` with `-XX:+UseCompactObjectHeaders` & `-XX:+UseSerialGC`), GitHub Actions (`.github/workflows/ci-cd.yml` with GCP Workload Identity Federation OIDC and `[deploy]` commit gate).

---

# 2. Hexagonal Component Map (`com.osvaldevops.notifications`)

## Domain Layer (`domain/` — Zero Spring Imports)
- **Models & Events:**
  - `domain/model/WelcomeNotification.java` (`id`, `userId`, `recipientEmail`, `recipientName`, `subject`, `body`, `status`, `sentAt`)
  - `domain/event/UserCreatedEvent.java` (`userId`, `email`, `userName`, `role`, `createdAt`)
- **Exceptions:**
  - `InvalidPubSubMessageException` (`400 Bad Request` — ACKed to prevent poison-pill infinite retries)
  - `NotificationDeliveryException` (`503 Service Unavailable` — NACKed so GCP Pub/Sub retries delivery with backoff)
- **Inbound Ports (`domain/port/in/`):**
  - `SendWelcomeNotificationUseCase` (`UserCreatedEvent` -> `WelcomeNotification`)
- **Outbound Ports (`domain/port/out/`):**
  - `NotificationSenderPort` (`send(WelcomeNotification)`)
- **Domain Services (`domain/service/`):**
  - `WelcomeNotificationDomainService`

## Infrastructure Layer (`infrastructure/`)
- **Inbound Messaging Adapters (`adapter/in/messaging/`):**
  - `PubSubPushConsumerAdapter` (`POST /api/v1/pubsub/events`)
  - `PubSubUserCreatedSubscriberAdapter` (Pull subscriber via `PubSubTemplate`)
  - `UserCreatedEventMapper` (MapStruct)
  - DTOs: `PubSubPushRequest`, `UserCreatedMessagePayload`
- **Inbound Web Adapter (`adapter/in/web/`):**
  - `NotificationController` (`POST /api/v1/notifications/welcome`)
  - `GlobalExceptionHandler` (`ApiErrorResponse`)
  - `NotificationWebMapper` (MapStruct)
  - DTOs: `SendNotificationRequest`, `NotificationResponse`, `ApiErrorResponse`
- **Outbound Notification Adapter (`adapter/out/notification/`):**
  - `EmailNotificationSenderAdapter` (`JavaMailSender` SMTP + simulation mode)
- **Configuration (`config/`):**
  - `BeanConfiguration` (wires `WelcomeNotificationDomainService`)
  - `OpenApiConfig` (Swagger UI metadata & relative server URL)

---

# 3. Environment Variables Contract (`application-dev.yml` & GitHub Secrets)
| Variable | Purpose |
| :--- | :--- |
| `GCP_PROJECT_ID` | Google Cloud Project ID for Pub/Sub |
| `PUBSUB_SUBSCRIBER_ENABLED` | Enables/disables the Pull subscriber (`true` / `false`) |
| `PUBSUB_USER_CREATED_SUBSCRIPTION` | GCP Pub/Sub subscription name (`user-created-sub`) |
| `MAIL_HOST` | SMTP server host (e.g., `smtp.resend.com`) |
| `MAIL_PORT` | SMTP server port (default `587`) |
| `MAIL_USERNAME` | SMTP username (e.g., `resend`) |
| `MAIL_PASSWORD` | SMTP API key / password |
| `NOTIFICATION_SENDER_EMAIL` | Sender `From` email address |
| `MAIL_ENABLED` | Enables real SMTP delivery (`true`) or simulation logging (`false`) |
| `PORT` | HTTP server port injected by Cloud Run (default `8082`) |

---

# 4. Context Synthesis Protocol (Instructions for the Agent)
Whenever a major iteration is completed in `notifications-service`:
1. Read this file first before scanning the repository to save context tokens.
2. If new ports, adapters, endpoints, or environment variables were added, update Sections **2** and **3**.
3. Append a concise entry under **Section 5 (Iteration Log)** summarizing the date, objective, modified files, and test/coverage status.

---

# 5. Iteration Log
- **Iteration 1 (Unit Testing & Java 25 Compatibility):** Configured `maven-surefire-plugin` with ByteBuddy experimental flag for Java 25 and JaCoCo `prepare-agent`, reaching 100% unit test coverage.
- **Iteration 2 (Kafka to GCP Pub/Sub Push & Pull + SMTP Mail):** Replaced Kafka consumer with `PubSubPushConsumerAdapter` and `PubSubUserCreatedSubscriberAdapter`, added `EmailNotificationSenderAdapter` with `JavaMailSender`, and removed hardcoded `@Value` fallbacks.
- **Iteration 3 (DevSecOps Secrets Masking, Cloud Run Memory Tuning & OpenAPI 3 Optimization):** Un-ignored `application-dev.yml` and `.antigravity/` in `.gitignore`, masked all GCP and SMTP credentials with `${ENV_VAR}` placeholders, enabled Java 25 Virtual Threads & `lazy-initialization: true`, configured Jib (`eclipse-temurin:25-jre-alpine` + `-XX:+UseCompactObjectHeaders`) with `[deploy]` commit gate in `ci-cd.yml`, and enriched OpenAPI 3 schemas.

