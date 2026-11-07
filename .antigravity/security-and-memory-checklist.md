# Role & Purpose
You are a **Security & Memory Optimization Auditor** dedicated specifically to `notifications-service`.
Use this checklist before finalizing code changes, tuning configurations, or preparing a release for **Google Cloud Run** (where memory footprint, CPU allocation, and Pub/Sub retry policies directly impact billing and cold-start latency).

---

# 1. `notifications-service` Security & Resilience Checklist

## A. Secrets & Configuration Isolation
- [x] **`application.yml` Ignored:** Confirm `src/main/resources/application.yml` is listed in `.gitignore` and never tracked by Git (`git status --ignored`).
- [x] **`application-dev.yml` Masked:** Confirm `src/main/resources/application-dev.yml` contains zero literal credentials—only `${GCP_PROJECT_ID}`, `${PUBSUB_USER_CREATED_SUBSCRIPTION}`, `${MAIL_HOST}`, `${MAIL_PORT:587}`, `${MAIL_USERNAME}`, `${MAIL_PASSWORD}`, and `${NOTIFICATION_SENDER_EMAIL}`.
- [x] **No `@Value` Fallbacks in Code:** Verify `PubSubUserCreatedSubscriberAdapter` and `EmailNotificationSenderAdapter` use `@Value("${...}")` without `:default` values hardcoded in Java.

## B. Cloud Run OIDC & Pub/Sub Push Protection
- [x] **Private Cloud Run Service (`--no-allow-unauthenticated`):** Confirm `.github/workflows/ci-cd.yml` deploys `notification-service` with `--no-allow-unauthenticated` and `PUBSUB_SUBSCRIBER_ENABLED=false` so only authenticated GCP Pub/Sub Push requests (via OIDC `roles/run.invoker`) can wake up the container or trigger email delivery.
- [x] **Exponential Backoff & Dead Lettering (DLQ):** Ensure the GCP Pub/Sub Push subscription (`user-created-sub`) is configured with `--min-retry-delay=10s --max-retry-delay=600s` and `--dead-letter-topic=user-created-dlq-topic --max-delivery-attempts=5` to prevent infinite retry loops from exhausting Cloud Run Free Tier CPU or Resend SMTP quotas.
- [x] **Keyless GCP Auth (OIDC):** Confirm `.github/workflows/ci-cd.yml` uses `workload_identity_provider` and `service_account` (`id-token: write`) instead of static Service Account JSON keys, and masks `cloud-run-sa@${{ secrets.GCP_PROJECT_ID }}.iam.gserviceaccount.com`.
- [x] **Actuator Surface Area:** Confirm `application-dev.yml` exposes only `health` (with `show-details: when_authorized`).

---

# 2. Memory & Performance Optimization Checklist (Cloud Run & Java 25)

## A. Object Allocation & Heap Efficiency in Java 25
- [x] **Immutable `record` DTOs & Events:** Keep all inbound/outbound DTOs (`PubSubPushRequest`, `UserCreatedMessagePayload`, `SendNotificationRequest`, `NotificationResponse`, `ApiErrorResponse`) and domain events (`UserCreatedEvent`) as Java `record` types to minimize object header overhead and GC pressure.
- [x] **Singleton Bean Reuse:** Never instantiate `ObjectMapper` or `JavaMailSender` per request; always inject and reuse singleton beans.

## B. Container & JVM Memory Tuning for Cloud Run
- [x] **Alpine JRE Base Image (Not Full JDK):** In Jib (`ci-cd.yml`), always use `eclipse-temurin:25-jre-alpine` instead of a full JDK image to reduce container image size (~95 MB) and Cloud Run cold-start latency.
- [x] **JVM Container Flags (Cloud Run `512Mi`):** Verify `-Djib.container.jvmFlags` includes `-XX:+UseCompactObjectHeaders,-XX:+UseSerialGC,-XX:MaxRAMPercentage=75.0,-XX:TieredStopAtLevel=1,-Djava.security.egd=file:/dev/./urandom`.
- [x] **Spring Boot Memory Diet:** Keep `spring.main.lazy-initialization: true`, `spring.main.banner-mode: off`, and `spring.threads.virtual.enabled: true` in `application-dev.yml`.

---

# 3. Audit Sign-Off
- **Last Audited:** `2026-11-06`
- **Status:** `PASSED (12/12 checks verified)` — Approved for Cloud Run deployment.

