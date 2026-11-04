# Notification Service (`Notification-service`)

Servicio de **Notificaciones** construido con **Java 25**, **Spring Boot 3.4.5**, **Arquitectura Hexagonal (Ports & Adapters)**, optimizado para **Google Cloud Run** consumiendo eventos mediante **Google Cloud Pub/Sub Push Subscriptions**, envío de correos vía **SMTP (`JavaMailSender`)**, documentado con **OpenAPI 3 / Swagger UI**, y asegurado en calidad con **JaCoCo** y **SonarQube**.

---

## 1. Tecnologías Principales

| Tecnología | Versión / Detalle |
| :--- | :--- |
| **Java** | 25 |
| **Spring Boot** | 3.4.5 |
| **Mensajería Cloud-Native** | Google Cloud Pub/Sub (Push HTTP Webhook para Cloud Run) |
| **Envío de Correos** | Spring Boot Starter Mail (`JavaMailSender` + modo simulación) |
| **Mapeo & Boilerplate** | MapStruct `1.6.3` + Lombok |
| **Documentación API** | SpringDoc OpenAPI `2.8.6` (Swagger UI) |
| **Calidad & Cobertura** | JaCoCo `0.8.13` (mínimo 80%) + SonarQube Scanner `5.0.0.4389` |
| **Testing** | JUnit 5, Mockito, AssertJ, Cucumber |

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
    │   │   └── infrastructure/
    │   │       ├── adapter/
    │   │       │   ├── in/
    │   │       │   │   ├── messaging/
    │   │       │   │   │   ├── PubSubPushConsumerAdapter.java
    │   │       │   │   │   ├── dto/
    │   │       │   │   │   │   ├── PubSubPushRequest.java
    │   │       │   │   │   │   └── UserCreatedMessagePayload.java
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
                │   │   ├── PubSubPushConsumerAdapterTest.java
                │   │   └── mapper/UserCreatedEventMapperTest.java
                │   └── web/
                │       ├── controller/NotificationControllerTest.java
                │       ├── exception/GlobalExceptionHandlerTest.java
                │       └── mapper/NotificationWebMapperTest.java
                └── out/notification/
                    └── EmailNotificationSenderAdapterTest.java
```

---

## 3. Integración con Google Cloud Pub/Sub Push y Cloud Run

1. `login-service` publica un evento en el topic de GCP Pub/Sub (con atributo opcional `eventType=USER_CREATED` y el JSON en `data`).
2. La suscripción **Push** de GCP Pub/Sub invoca `POST /api/v1/pubsub/events` en la instancia de **Cloud Run** (permitiendo escalar a `0` instancias cuando no hay tráfico).
3. `PubSubPushConsumerAdapter` decodifica `message.data` desde Base64, enruta según `message.attributes.eventType` (`USER_CREATED`) y ejecuta `SendWelcomeNotificationUseCase`.
4. Si el procesamiento es exitoso, devuelve `204 No Content` (**ACK** automático en Pub/Sub). Si ocurre un fallo de entrega SMTP (`503 Service Unavailable`), Pub/Sub hace **NACK** y reintenta con *exponential backoff*.

### Ejemplo de petición Push de GCP Pub/Sub (`POST /api/v1/pubsub/events`)
```json
{
  "message": {
    "data": "eyJ1c2VySWQiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJlbWFpbCI6Im9zdmFsQG9zdmFsZGV2b3BzLmNvbSIsInVzZXJOYW1lIjoib3N2YWwiLCJyb2xlIjoiUk9MRV9VU0VSIiwiY3JlYXRlZEF0IjoiMjAyNi0xMC0zMFQxOTowMDowMFoifQ==",
    "messageId": "1058293847192",
    "publishTime": "2026-11-04T23:30:00Z",
    "attributes": {
      "eventType": "USER_CREATED"
    }
  },
  "subscription": "projects/osvaldevops-gcp/subscriptions/user-created-events-sub"
}
```
*(El campo `data` corresponde al JSON `{"userId":"550e8400-e29b-41d4-a716-446655440000","email":"osval@osvaldevops.com","userName":"osval","role":"ROLE_USER","createdAt":"2026-10-30T19:00:00Z"}` codificado en Base64).*
