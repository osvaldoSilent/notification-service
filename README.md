# Notification Service (`Notification-service`)

Servicio de **Notificaciones** construido con **Java 25**, **Spring Boot 3.4.5**, **Arquitectura Hexagonal (Ports & Adapters)**, **Apache Kafka Consumer**, documentado con **OpenAPI 3 / Swagger UI**, y asegurado en calidad con **JaCoCo** y **SonarQube**.

Escucha asíncronamente los eventos `UserCreatedEvent` publicados por [`login-service`](../login-service) en el topic `user-created-events` y despacha notificaciones de bienvenida a los usuarios recién registrados.

---

## 1. Tecnologías Principales

| Tecnología | Versión / Detalle |
| :--- | :--- |
| **Java** | 25 |
| **Spring Boot** | 3.4.5 |
| **Mensajería Asíncrona** | Spring for Apache Kafka (`@KafkaListener`) |
| **Mapeo & Boilerplate** | MapStruct `1.6.3` + Lombok |
| **Documentación API** | SpringDoc OpenAPI `2.8.6` (Swagger UI) |
| **Calidad & Cobertura** | JaCoCo `0.8.13` (mínimo 80%) + SonarQube Scanner `5.0.0.4389` |
| **Testing** | JUnit 5, Mockito, AssertJ |

---

## 2. Arquitectura Hexagonal (`com.osvaldevops.notifications`)

```text
notifications-service/
├── .gitignore
├── compose.yaml
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/osvaldevops/notifications/
    │   │   ├── NotificationServiceApplication.java
    │   │   ├── domain/
    │   │   │   ├── event/
    │   │   │   │   └── UserCreatedEvent.java
    │   │   │   ├── exception/
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
    │   │   └── infrastructure/
    │   │       ├── adapter/
    │   │       │   ├── in/
    │   │       │   │   ├── messaging/
    │   │       │   │   │   ├── KafkaUserCreatedConsumerAdapter.java
    │   │       │   │   │   ├── dto/UserCreatedMessagePayload.java
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
    │       ├── application.yml
    │       └── application-dev.yml
    └── test/
        └── java/com/osvaldevops/notifications/
            ├── NotificationServiceApplicationTests.java
            ├── domain/service/
            │   └── WelcomeNotificationDomainServiceTest.java
            └── infrastructure/adapter/
                ├── in/
                │   ├── messaging/
                │   │   ├── KafkaUserCreatedConsumerAdapterTest.java
                │   │   └── mapper/UserCreatedEventMapperTest.java
                │   └── web/
                │       ├── controller/NotificationControllerTest.java
                │       ├── exception/GlobalExceptionHandlerTest.java
                │       └── mapper/NotificationWebMapperTest.java
                └── out/notification/
                    └── EmailNotificationSenderAdapterTest.java
```

---

## 3. Flujo de Integración con `login-service` vía Apache Kafka

1. `login-service` (puerto `8081`) crea un usuario en `POST /api/v1/auth/register` y publica un mensaje JSON en el topic `user-created-events`.
2. `KafkaUserCreatedConsumerAdapter` en `Notification-service` (puerto `8082`) escucha el topic `user-created-events` (grupo `notification-service-group`).
3. Mapea el payload a `UserCreatedEvent` e invoca `SendWelcomeNotificationUseCase`.
4. `WelcomeNotificationDomainService` construye la notificación de bienvenida y la envía mediante `NotificationSenderPort` (`EmailNotificationSenderAdapter`).

---

## 4. Documentación Interactiva con Swagger UI / OpenAPI

Con la aplicación en ejecución (puerto `8082`):

- **Swagger UI:** http://localhost:8082/swagger-ui.html
- **OpenAPI JSON Spec:** http://localhost:8082/v3/api-docs
- **Endpoint de prueba manual:** `POST /api/v1/notifications/welcome`

