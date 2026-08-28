# Async Notification System

A full-stack, production-oriented asynchronous bulk notification and delivery tracking system built using **Spring Boot, React, MongoDB, RabbitMQ, and SMTP**.

The project demonstrates how a synchronous bulk email workflow can be redesigned using message-driven asynchronous processing to improve API responsiveness, reliability, failure handling, and delivery visibility.

---

## Problem Statement

Consider a newsletter system where an administrator needs to send an email notification to all subscribed users.

A simple implementation could process every recipient synchronously:

```text
Client
  ↓
POST /notifications
  ↓
Fetch Subscribers
  ↓
Send Email 1
  ↓
Send Email 2
  ↓
Send Email 3
  ↓
...
  ↓
Return HTTP Response
```

The HTTP request remains occupied until all recipients are processed.

During the initial synchronous implementation, a test with **11 recipients** and simulated downstream email latency resulted in an API response time of approximately:

> **6.31 seconds**

As the subscriber count grows, this approach does not provide a good user-facing API experience.

The notification workflow was therefore redesigned using **RabbitMQ** to decouple notification delivery from the HTTP request.

---

## Solution

The asynchronous implementation follows this flow:

```text
React
  ↓
Spring Boot REST API
  ↓
Create Notification
  ↓
Create PENDING delivery records
  ↓
Publish messages to RabbitMQ
  ↓
202 Accepted
```

Email delivery then continues independently:

```text
RabbitMQ
   ↓
Notification Consumer
   ↓
Gmail SMTP
   ↓
Email
   ↓
Update delivery status
   ↓
SENT / FAILED
```

This allows the API to return without waiting for every email delivery operation to finish.

---

## Architecture

```mermaid
flowchart LR

    UI[React UI]

    API[Spring Boot REST API]

    DB[(MongoDB)]

    MQ[RabbitMQ<br/>notification.queue]

    CONSUMER[Notification Consumer]

    SMTP[Gmail SMTP]

    RETRY[Retry + Exponential Backoff]

    DLX[Dead Letter Exchange]

    DLQ[Dead Letter Queue]

    FAILURE[Failure Consumer]

    UI -->|REST API| API

    API --> DB

    API -->|Publish notification messages| MQ

    MQ --> CONSUMER

    CONSUMER --> SMTP

    CONSUMER -->|Update SENT status| DB

    CONSUMER -->|Processing failure| RETRY

    RETRY -->|Retry| CONSUMER

    RETRY -->|Retries exhausted| DLX

    DLX --> DLQ

    DLQ --> FAILURE

    FAILURE -->|Update FAILED status| DB

    UI -->|Poll delivery status| API
```

---

## Application Flow

### 1. Subscriber Registration

Users can subscribe individually using their email address.

```text
React
  ↓
POST /api/subscribers
  ↓
Spring Boot
  ↓
MongoDB
```

Subscriber emails are protected by a **unique MongoDB index** to prevent duplicate records.

Subscribers can also be marked inactive when they unsubscribe.

---

### 2. Bulk Subscriber Import

Administrators can upload subscribers using a CSV file.

Example:

```csv
email
user1@example.com
user2@example.com
user3@example.com
```

The import process:

```text
CSV Upload
   ↓
Stream CSV Records
   ↓
Normalize Email
   ↓
Validate
   ↓
Detect In-File Duplicates
   ↓
Process Batch
   ↓
Bulk Lookup Existing Subscribers
   ↓
Bulk Save New Subscribers
   ↓
MongoDB
```

The API returns an import summary:

```json
{
  "totalRecords": 11,
  "imported": 6,
  "duplicates": 4,
  "invalid": 1
}
```

### Bulk Import Design

The implementation uses:

- Apache Commons CSV for CSV parsing
- Streaming file processing
- Header-based field extraction
- `HashSet` for in-file duplicate detection
- Batch processing
- MongoDB bulk lookup using `findByEmailIn(...)`
- `saveAll(...)` for batch persistence
- Unique database index as the final consistency guarantee
- File-size limits to protect the upload endpoint

This avoids performing an individual database lookup and insert for every CSV row.

The test file deliberately included valid records, duplicate/existing subscriber emails, and an invalid email address.

![Bulk CSV Import](screenshots/csv-import.png)

In this test:

| Result | Count |
|---|---:|
| Total Records | 11 |
| Imported | 6 |
| Duplicates | 4 |
| Invalid | 1 |

---

## Asynchronous Notification Processing

When an administrator submits a newsletter:

```text
POST /api/notifications
```

the backend:

1. Creates the notification.
2. Retrieves active subscribers.
3. Creates a `PENDING` delivery record for every recipient.
4. Publishes a notification message to RabbitMQ for each recipient.
5. Returns **HTTP 202 Accepted**.

Example RabbitMQ message:

```json
{
  "notificationId": "68ad...",
  "email": "subscriber@example.com",
  "subject": "Product Update",
  "message": "New features are now available."
}
```

The RabbitMQ consumer independently processes each message and sends the email through SMTP.

---

## Delivery Tracking

Asynchronous processing means the initial API response does not indicate that every email has already been delivered.

Therefore, each recipient has an individual delivery record in MongoDB.

Supported states:

```text
PENDING
   ↓
 SENT
```

or:

```text
PENDING
   ↓
Retry
   ↓
FAILED
```

The frontend retrieves progress using:

```http
GET /api/notifications/{notificationId}/status
```

Example response while processing:

```json
{
  "notificationId": "68ad...",
  "total": 7,
  "pending": 3,
  "sent": 4,
  "failed": 0
}
```

The React frontend polls this endpoint every two seconds and updates the delivery progress automatically.

Polling stops once:

```text
pending = 0
```

### Live Asynchronous Processing

The following screenshot captures the notification while RabbitMQ consumers are still processing messages.

![Notification Processing](screenshots/dashboard-processing.png)

At this point:

```text
Total:      7
Sent:       4
Pending:    3
Failed:     0
Processed: 57%
```

The original HTTP request has already completed, while delivery processing continues asynchronously in the background.

### Processing Completed

Once all recipients have been processed, `pending` reaches zero and the frontend stops polling.

![Notification Processing Completed](screenshots/dashboard-completed.png)

Final state:

```text
Total:      7
Sent:       7
Pending:    0
Failed:     0
Processed: 100%
```

This demonstrates the distinction between **HTTP request completion** and **background processing completion**.

---

## Retry and Dead-Letter Handling

Email providers and external services can fail temporarily.

The RabbitMQ listener therefore uses bounded retries with exponential backoff.

```text
Message
  ↓
Consumer
  ↓
Attempt 1 ❌
  ↓
Wait
  ↓
Attempt 2 ❌
  ↓
Wait Longer
  ↓
Attempt 3 ❌
  ↓
Retries Exhausted
  ↓
Dead Letter Exchange
  ↓
Dead Letter Queue
  ↓
Failure Consumer
  ↓
MongoDB → FAILED
```

The implementation prevents permanently failing messages from being retried indefinitely.

A failed delivery retains failure information in MongoDB so RabbitMQ is not used as the application's historical business-state store.

### RabbitMQ Queue and DLQ

The application maintains the primary notification queue and a dedicated dead-letter queue.

![RabbitMQ Queue and DLQ](screenshots/rabbitmq-dlq.png)

The primary `notification.queue` is configured with dead-letter routing. When retry attempts are exhausted, the rejected message is routed through the dead-letter exchange toward `notification.dlq`.

### Failure Tracking

Failure handling is also reflected in the application's persistent delivery state.

![Delivery Failure Tracking](screenshots/delivery-failure.png)

In the failure-handling test:

```text
Total:    11
Sent:     10
Pending:   0
Failed:    1
```

The permanently failed delivery is persisted as `FAILED`, allowing the status API and frontend to expose the final business outcome.

> RabbitMQ is responsible for message transport and failure routing, while MongoDB stores the application's delivery state and history.

---

## Email Delivery

Emails are sent using Spring's `JavaMailSender` through Gmail SMTP.

```text
RabbitMQ Consumer
       ↓
Email Service
       ↓
JavaMailSender
       ↓
Gmail SMTP
       ↓
Recipient
```

A delivery is marked `SENT` after the SMTP send operation completes successfully.

### Real Email Delivery

![Real Email Delivery](screenshots/gmail-delivery.png)

The screenshot demonstrates an actual notification submitted through the application and delivered to a Gmail inbox after asynchronous processing through Spring Boot, RabbitMQ, and Gmail SMTP.

> SMTP acceptance indicates that the provider accepted the send operation; it does not guarantee that the recipient opened or read the email.

---

## Synchronous vs Asynchronous Processing

The functionality was initially implemented synchronously to establish a measurable baseline before introducing RabbitMQ.

### Synchronous Implementation

With **11 recipients** and simulated downstream email latency:

```text
11 recipients
      ↓
Sequential processing
      ↓
API waits for completion
      ↓
Response time ≈ 6.31 seconds
```

![Synchronous API Response](screenshots/synchronous-response.png)

The HTTP request remained blocked while all recipient operations were processed.

### RabbitMQ Implementation

After introducing RabbitMQ:

```text
HTTP Request
     ↓
Create delivery records
     ↓
Publish messages
     ↓
Return response

-------------------------

RabbitMQ
     ↓
Consumers
     ↓
Email processing continues independently
```

In one local test run, the asynchronous API returned in approximately **26 ms**, while delivery processing continued in the background.

![Asynchronous API Response](screenshots/async-response.png)

### Observed Comparison

| Implementation | Test Recipients | Observed API Response |
|---|---:|---:|
| Synchronous | 11 | ~6.31 seconds |
| RabbitMQ-based asynchronous | 11 | ~26 ms |

> These figures represent individual local test runs and are included to demonstrate the behavioral difference between synchronous and asynchronous request processing. They are not production performance guarantees.

> RabbitMQ reduces user-facing request latency by decoupling long-running work from the HTTP request. It does **not** make the underlying email-delivery operation instantaneous.

---

## Frontend

The React dashboard provides:

- Individual subscriber registration
- CSV bulk subscriber import
- Newsletter submission
- Live delivery progress
- Sent / Pending / Failed counters
- Processing progress indicator
- Success and failure feedback

The frontend uses React hooks including:

- `useState`
- `useEffect`

When a newsletter is submitted:

```text
NewsletterForm
      ↓
POST /api/notifications
      ↓
notificationId
      ↓
App state
      ↓
DeliveryStatus
      ↓
useEffect
      ↓
GET status every 2 seconds
      ↓
Update React state
      ↓
Re-render progress
      ↓
pending = 0
      ↓
Stop polling
```

This keeps the UI updated while asynchronous processing continues without requiring the user to manually refresh the page.

---

## Screenshots

The following screenshots are used throughout this README as evidence of the implemented flows.

| Screenshot | Demonstrates |
|---|---|
| `dashboard-processing.png` | React displaying asynchronous processing at 57% |
| `dashboard-completed.png` | Delivery processing completed at 100% |
| `synchronous-response.png` | ~6.31 second synchronous baseline |
| `async-response.png` | ~26 ms asynchronous API response in a local test |
| `rabbitmq-dlq.png` | RabbitMQ primary queue and dead-letter queue |
| `delivery-failure.png` | Persistent result with 10 SENT and 1 FAILED |
| `csv-import.png` | Bulk CSV validation, duplicate detection, and import summary |
| `gmail-delivery.png` | Actual Gmail SMTP email delivery |

---

## Technology Stack

### Backend

- Java
- Spring Boot
- Spring Web MVC
- Spring Data MongoDB
- Spring AMQP
- Spring Mail
- Bean Validation
- Spring Boot Actuator
- Apache Commons CSV
- Lombok

### Frontend

- React
- JavaScript
- Vite
- Axios
- CSS

### Infrastructure

- MongoDB
- RabbitMQ / CloudAMQP
- Gmail SMTP

### Development Tools

- IntelliJ IDEA
- VS Code
- Postman
- MongoDB Compass
- Git / GitHub

---

## API Overview

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/subscribers` | Register subscriber |
| `DELETE` | `/api/subscribers/{email}` | Unsubscribe user |
| `POST` | `/api/subscribers/import` | Bulk import subscribers using CSV |
| `POST` | `/api/notifications` | Submit notification for asynchronous processing |
| `GET` | `/api/notifications/{id}/status` | Retrieve delivery progress |
| `GET` | `/actuator/health` | Application health |

---

## Configuration and Secrets

Credentials are **not stored in source control**.

Spring Boot configuration references environment variables.

Required:

```text
RABBITMQ_URL
MAIL_USERNAME
MAIL_APP_PASSWORD
```

Optional/local configuration:

```text
MONGODB_URI
MONGODB_DATABASE
CORS_ALLOWED_ORIGIN
MAIL_HOST
MAIL_PORT
```

Example:

```properties
spring.rabbitmq.addresses=${RABBITMQ_URL}

spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_APP_PASSWORD}

spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/notification_system_db}
```

For local development, secrets can be supplied through the IDE/runtime environment.

For production deployments, secrets should be supplied through the deployment platform or a dedicated secrets-management solution rather than committed to the repository.

Examples include:

- AWS Secrets Manager
- Azure Key Vault
- HashiCorp Vault
- Kubernetes Secrets

---

## Running Locally

### Prerequisites

- Java
- Maven
- Node.js / npm
- MongoDB
- RabbitMQ connection
- SMTP credentials

### 1. Configure Environment Variables

Set:

```text
RABBITMQ_URL=<rabbitmq-connection-url>
MAIL_USERNAME=<smtp-username>
MAIL_APP_PASSWORD=<smtp-app-password>
```

MongoDB defaults to:

```text
mongodb://localhost:27017/notification_system_db
```

### 2. Start Backend

```bash
cd backend
mvn spring-boot:run
```

Backend:

```text
http://localhost:8080
```

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

---

## Project Structure

```text
async-notification-system/
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── ...
│
├── frontend/
│   ├── src/
│   ├── package.json
│   └── ...
│
├── screenshots/
│   ├── dashboard-processing.png
│   ├── dashboard-completed.png
│   ├── synchronous-response.png
│   ├── async-response.png
│   ├── rabbitmq-dlq.png
│   ├── delivery-failure.png
│   ├── csv-import.png
│   └── gmail-delivery.png
│
└── README.md
```

---

## Key Engineering Decisions

**Why RabbitMQ instead of synchronous email processing?**

Email delivery is an external, comparatively slow operation. RabbitMQ decouples it from the HTTP request lifecycle so the API does not remain blocked while every recipient is processed.

**Why HTTP 202 Accepted?**

The server has accepted the notification request, but asynchronous processing may still be in progress when the response is returned.

**Why store delivery state in MongoDB?**

RabbitMQ handles message transport and work distribution. MongoDB maintains application business state and delivery history.

**Why a DLQ?**

Messages that repeatedly fail should not be retried indefinitely or interfere with normal processing. Dead-letter handling isolates permanently failing messages.

**Why CSV streaming and batching?**

It reduces unnecessary memory usage and database round trips compared with materializing all Subscriber entities and performing one query/write per row.

The implementation still maintains a `HashSet` of normalized emails for within-file duplicate detection, so memory usage for duplicate tracking grows with the number of unique records. The upload endpoint therefore has a bounded file-size limit.

**Why Apache Commons CSV instead of manually splitting lines?**

CSV supports quoting, escaping, delimiters, and multiple columns. Apache Commons CSV handles these rules and allows the application to retrieve the required value by header name, for example:

```java
record.get("email")
```

instead of depending on a fixed column position or using `String.split(",")`.

**Why database uniqueness if duplicates are already checked in Java?**

Application checks provide convenient duplicate handling, while the database unique index provides the final consistency guarantee against concurrent requests.

**Why polling instead of WebSocket/SSE?**

Delivery updates do not require sub-second real-time communication for this scope. Short-lived polling keeps the frontend/backend design simple, and polling stops automatically when processing completes.

For larger-scale or highly real-time requirements, Server-Sent Events or WebSockets could be considered.

---

## Current Scope

This project focuses on the notification-processing capability itself.

Potential extensions include:

- Authentication and role-based authorization
- Multiple notification channels
- HTML email templates
- Multiple RabbitMQ consumers / horizontal scaling
- WebSocket or Server-Sent Events instead of polling
- Notification scheduling
- Provider abstraction for different email services
- Metrics and distributed tracing
- Transactional Outbox for stronger database/message consistency
- Idempotent consumer handling for duplicate message delivery
- Asynchronous processing for extremely large CSV imports

---

## Production Considerations

The current implementation demonstrates production-oriented patterns while intentionally keeping the project scope manageable.

For a larger production deployment, additional considerations would include:

- **Transactional Outbox** to handle the consistency gap between database persistence and RabbitMQ publishing.
- **Idempotent consumers** because message brokers can redeliver messages.
- **Production email providers** such as Amazon SES or similar services instead of a personal Gmail SMTP account.
- **Consumer concurrency and prefetch tuning** based on workload and downstream rate limits.
- **Centralized metrics and monitoring** for queue depth, retries, DLQ messages, delivery failures, API latency, JVM health, and SMTP latency.
- **Authentication and authorization** for administrative notification and bulk-import endpoints.
- **Asynchronous import jobs/object storage** for very large subscriber files.

These are architectural evolution points rather than claims that the current demonstration implements every large-scale production concern.

---

## What This Project Demonstrates

The project focuses on practical backend and full-stack engineering concepts:

- Synchronous vs asynchronous processing
- Message-driven architecture
- Producer/consumer communication
- RabbitMQ queue processing
- Retry and exponential backoff
- Dead-letter exchange and dead-letter queue
- REST API design
- HTTP `202 Accepted`
- Eventual consistency
- Delivery-state tracking
- Bulk CSV processing
- Streaming file parsing
- Database batching
- Duplicate handling
- Database uniqueness
- External SMTP integration
- Runtime configuration and secret management
- React state management
- React `useEffect` lifecycle and cleanup
- Status API polling
- CORS configuration
- End-to-end full-stack integration

---

## Summary

This project started with a simple synchronous bulk notification workflow and evolved into an asynchronous, observable, and failure-aware notification-processing system.

The main engineering objective was not simply to introduce RabbitMQ, but to address the broader concerns created by long-running external operations:

```text
Responsiveness
      +
Asynchronous Processing
      +
Failure Handling
      +
Delivery Visibility
      +
Bulk Data Processing
```

The resulting implementation demonstrates how Spring Boot, RabbitMQ, MongoDB, SMTP, and React can work together to provide a practical full-stack notification capability.