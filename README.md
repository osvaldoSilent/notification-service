# Notification Service (`notifications-service`)

Servicio Cloud-Native de **Notificaciones por Correo Electrónico** construido con **Java 25**, **Spring Boot 3.4.5**, **Arquitectura Hexagonal Pura (Ports & Adapters)**, integrado con **Google Cloud Pub/Sub (Push Webhook & Pull Subscriber)** y **Spring Boot Mail (`JavaMailSender` vía SMTP / Resend)**, optimizado para bajo consumo de memoria en **Google Cloud Run**, documentado con **OpenAPI 3 / Swagger UI**, y asegurado con **JaCoCo (100% cobertura)** y **SonarCloud**.

---

## 1. Stack Tecnológico

| Categoría | Tecnología | Versión / Detalle |
| :--- | :--- | :--- |
| **Lenguaje & Runtime** | Java (Eclipse Temurin) | **25** (Virtual Threads + Compact Object Headers `-XX:+UseCompactObjectHeaders`) |
| **Framework Base** | Spring Boot | **3.4.5** (`lazy-initialization: true`, `banner-mode: off`) |
| **Mensajería Asíncrona** | Spring Cloud GCP Pub/Sub | `spring-cloud-gcp-starter-pubsub` `5.9.0` (Push HTTP Webhook + Pull Subscriber) |
| **Envío de Correos** | Spring Boot Starter Mail | `JavaMailSender` (SMTP STARTTLS con Resend / Mailpit local + modo simulación) |
| **Mapeo & Boilerplate** | MapStruct + Lombok | MapStruct `1.6.3` (`componentModel = "spring"`) |
| **Documentación API** | SpringDoc OpenAPI 3 | `springdoc-openapi-starter-webmvc-ui` `2.8.6` (`/swagger-ui.html`, `/v3/api-docs`) |
| **Calidad & Testing** | JUnit 5, Mockito, AssertJ, Cucumber, JaCoCo, SonarQube | **28 unit tests** (100% cobertura JaCoCo en las 10 clases analizadas) + SonarCloud Quality Gate |
| **Contenedor & CI/CD** | Jib + GitHub Actions + Cloud Run | `eclipse-temurin:25-jre-alpine` + Workload Identity Federation (OIDC) + `[deploy]` gate |

---

## 2. Arquitectura Hexagonal Pura (`com.osvaldevops.notifications`)

El paquete `domain` es **100% Java puro** (cero dependencias de `org.springframework.*`). El ensamblaje del caso de uso se realiza exclusivamente en [`BeanConfiguration`](src/main/java/com/osvaldevops/notifications/infrastructure/config/BeanConfiguration.java).

```text
notifications-service/
├── .antigravity/
│   ├── context-synthesizer.md
│   └── security-and-memory-checklist.md
├── .github/workflows/
│   └── ci-cd.yml
├── compose.yaml
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/osvaldevops/notifications/
    │   │   ├── NotificationServiceApplication.java
    │   │   ├── domain/                                  # 100% Pure Java (Zero Spring imports)
    │   │   │   ├── event/
    │   │   │   │   └── UserCreatedEvent.java
    │   │   │   ├── exception/
    │   │   │   │   ├── InvalidPubSubMessageException.java
    │   │   │   │   └── NotificationDeliveryException.java
    │   │   │   ├── model/
    │   │   │   │   └── WelcomeNotification.java
    │   │   │   ├── port/
    │   │   │   │   ├── in/
    │   │   │   │   │   └── SendWelcomeNotificationUseCase.java
    │   │   │   │   └── out/
    │   │   │   │       └── NotificationSenderPort.java
    │   │   │   └── service/
    │   │   │       └── WelcomeNotificationDomainService.java
    │   │   └── infrastructure/                          # Frameworks, Drivers & Adapters
    │   │       ├── adapter/
    │   │       │   ├── in/
    │   │       │   │   ├── messaging/
    │   │       │   │   │   ├── PubSubPushConsumerAdapter.java
    │   │       │   │   │   ├── PubSubUserCreatedSubscriberAdapter.java
    │   │       │   │   │   ├── dto/ (PubSubPushRequest, UserCreatedMessagePayload)
    │   │       │   │   │   └── mapper/UserCreatedEventMapper.java
    │   │       │   │   └── web/
    │   │       │   │       ├── controller/NotificationController.java
    │   │       │   │       ├── dto/ (SendNotificationRequest, NotificationResponse, ApiErrorResponse)
    │   │       │   │       ├── exception/GlobalExceptionHandler.java
    │   │       │   │       └── mapper/NotificationWebMapper.java
    │   │       │   └── out/
    │   │       │       └── notification/
    │   │       │           └── EmailNotificationSenderAdapter.java
    │   │       └── config/
    │   │           ├── BeanConfiguration.java
    │   │           └── OpenApiConfig.java
    │   └── resources/
    │       ├── application.yml                          # Ignorado en Git (desarrollo local)
    │       └── application-dev.yml                      # Versionado con variables ${ENV_VAR}
    └── test/
        └── java/com/osvaldevops/notifications/          # 28 Unit Tests (100% JaCoCo Coverage)
```

---

## 3. Contrato REST API & OpenAPI 3 (Swagger UI)

Con el servicio en ejecución, la documentación interactiva está disponible en:
- **Swagger UI:** `http://localhost:8082/swagger-ui.html`
- **OpenAPI JSON Spec:** `http://localhost:8082/v3/api-docs`

| Método | Endpoint | Descripción | Respuestas Documentadas |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/pubsub/events` | Webhook Push para suscripciones de Google Cloud Pub/Sub en Cloud Run. | `204 No Content` (ACK), `400`, `500`, `503` (`ApiErrorResponse`) |
| `POST` | `/api/v1/notifications/welcome` | Endpoint REST para disparo directo/prueba de notificación de bienvenida. | `201 Created` (`NotificationResponse`), `400`, `500`, `503` (`ApiErrorResponse`) |

---

## 4. Integración con Google Cloud Pub/Sub (Push & Pull) y SMTP

1. [`login-service`](../login-service) publica el evento `UserCreatedEvent` en el topic de GCP Pub/Sub con atributo `eventType=USER_CREATED`.
2. `notifications-service` soporta ambos modos de consumo:
   - **Push HTTP (`POST /api/v1/pubsub/events`)**: [`PubSubPushConsumerAdapter`](src/main/java/com/osvaldevops/notifications/infrastructure/adapter/in/messaging/PubSubPushConsumerAdapter.java) decodifica `message.data` desde Base64, valida el evento y devuelve `204 No Content` (**ACK**).
   - **Pull (`PubSubTemplate`)**: [`PubSubUserCreatedSubscriberAdapter`](src/main/java/com/osvaldevops/notifications/infrastructure/adapter/in/messaging/PubSubUserCreatedSubscriberAdapter.java) escucha activamente la suscripción `${PUBSUB_USER_CREATED_SUBSCRIPTION}` cuando `PUBSUB_SUBSCRIBER_ENABLED=true`.
3. [`EmailNotificationSenderAdapter`](src/main/java/com/osvaldevops/notifications/infrastructure/adapter/out/notification/EmailNotificationSenderAdapter.java) envía el correo de bienvenida mediante SMTP (`JavaMailSender`) o registra el envío en modo simulación si `MAIL_ENABLED=false`.

---

## 5. Seguridad, Gestión de Secretos y Optimización de Memoria (Cloud Run)

- **Aislamiento de Secretos**: `src/main/resources/application.yml` está ignorado en [`.gitignore`](.gitignore); [`src/main/resources/application-dev.yml`](src/main/resources/application-dev.yml) enmascara todas las credenciales con variables `${ENV_VAR}` (`${GCP_PROJECT_ID}`, `${PUBSUB_USER_CREATED_SUBSCRIPTION}`, `${MAIL_HOST}`, `${MAIL_PORT:587}`, `${MAIL_USERNAME}`, `${MAIL_PASSWORD}`, `${NOTIFICATION_SENDER_EMAIL}`).
- **Sin Fallbacks Hardcodeados en Java**: Los adaptadores exigen explícitamente sus propiedades desde archivos de configuración/entorno.
- **Optimización de Memoria y Cold-Start (Java 25 + Cloud Run `512Mi`)**:
  - Imagen `eclipse-temurin:25-jre-alpine` (~95 MB) construida con Jib.
  - Flags JVM: `-XX:+UseCompactObjectHeaders`, `-XX:+UseSerialGC`, `-XX:MaxRAMPercentage=75.0`, `-XX:TieredStopAtLevel=1`.
  - `spring.main.lazy-initialization: true`, `spring.main.banner-mode: off` y `spring.threads.virtual.enabled: true`.

---

## 6. Documentación de Agentes y Reglas de Ingeniería (`.antigravity/*.md`)

### A. Específicos de `notifications-service` ([`notifications-service/.antigravity/`](.antigravity))
| Archivo `.md` | Descripción |
| :--- | :--- |
| [`.antigravity/context-synthesizer.md`](.antigravity/context-synthesizer.md) | **Sintetizador de Contexto por Iteración:** Snapshot arquitectónico, mapa de componentes hexagonales, contrato de variables de entorno e historial de iteraciones de `notifications-service`. |
| [`.antigravity/security-and-memory-checklist.md`](.antigravity/security-and-memory-checklist.md) | **Checklist de Seguridad y Memoria:** Auditoría pre-despliegue de secretos SMTP/GCP, manejo de poison-pills en Pub/Sub y banderas JVM/Jib de ahorro de RAM en Cloud Run. |

### B. Globales del Workspace ([`../.antigravity/`](../.antigravity))
| Archivo `.md` | Descripción |
| :--- | :--- |
| [`../.antigravity/spring_boot_init.md`](../.antigravity/spring_boot_init.md) | **Blueprint de Inicialización Cloud-Native:** Estándares base para microservicios con Java 25, Spring Boot 3.4+, GCP Pub/Sub y despliegue serverless. |
| [`../.antigravity/hexagonal-validator.md`](../.antigravity/hexagonal-validator.md) | **Validador de Arquitectura Hexagonal:** Reglas estrictas de pureza del paquete `domain` (cero imports de Spring) y ensamblaje mediante `BeanConfiguration`. |
| [`../.antigravity/unit-test-agent.md`](../.antigravity/unit-test-agent.md) | **Agente de Pruebas Unitarias:** Guía para tests unitarios con JUnit 5, Mockito, AssertJ y configuración Surefire/ByteBuddy en Java 25 para alcanzar $\ge 80\%$ (100% actual) en JaCoCo. |
| [`../.antigravity/security-and-secrets-auditor.md`](../.antigravity/security-and-secrets-auditor.md) | **Auditor DevSecOps y Secretos:** Prevención de fugas de credenciales, uso de variables `${ENV_VAR}`, GitHub Secrets y Workload Identity Federation (OIDC). |
| [`../.antigravity/gcp-cloudrun-pubsub-architect.md`](../.antigravity/gcp-cloudrun-pubsub-architect.md) | **Arquitecto GCP Cloud Run & Pub/Sub:** Buenas prácticas de publicación/suscripción idempotente en Pub/Sub, Jib y configuración de escalado en Cloud Run. |
| [`../.antigravity/bdd-cucumber-agent.md`](../.antigravity/bdd-cucumber-agent.md) | **Agente BDD con Cucumber:** Lineamientos para escenarios Gherkin (`.feature`) y pruebas de aceptación desacopladas de infraestructura externa. |
| [`../.antigravity/sonarqube-quality-gate.md`](../.antigravity/sonarqube-quality-gate.md) | **Quality Gate de SonarQube/SonarCloud:** Reglas para mantener `0` Bugs, `0` Vulnerabilities, `0` Security Hotspots y código limpio en el pipeline CI/CD. |
| [`../.antigravity/api-contract-and-docs.md`](../.antigravity/api-contract-and-docs.md) | **Estándar de Contratos REST y OpenAPI 3:** Reglas de diseño de DTOs (`record`), manejo centralizado de errores (`ApiErrorResponse`) y documentación Swagger UI. |
| [`../.antigravity/code-generation-behavior.md`](../.antigravity/code-generation-behavior.md) | **Disciplina de Generación de Código (Minimal Diff):** Regla de comportamiento para realizar cambios quirúrgicos enfocados exclusivamente en los archivos requeridos por la lógica solicitada. |

---

## 7. Comandos de Ejecución y Verificación

```powershell
# Ejecutar suite completa de pruebas unitarias + reporte y verificación JaCoCo (fase verify)
mvn verify

# Levantar dependencias locales (Emulador Pub/Sub, Mailpit SMTP, SonarQube)
docker compose up -d
```
