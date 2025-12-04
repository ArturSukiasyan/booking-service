# Booking Service

Spring Boot 3 monolith with a REST API for bookings, Liquibase-managed PostgreSQL schema, Redis-backed availability cache, and an application-scheduled TTL for unpaid bookings.

## Running locally
1. Start PostgreSQL and Redis via docker-compose:
   ```bash
   docker compose up -d
   ```
2. Run the application (Java 21):
   ```bash
   ./gradlew bootRun
   ```
   or use a local Gradle installation with `gradle bootRun`.

Swagger UI: `http://localhost:8080/api/v1/swagger-ui/index.html` (OpenAPI at `/api-docs`).

## Booking lifecycle and TTL
- Creating a booking sets status `PENDING_PAYMENT` and `expires_at = now + 15 minutes`.
- Payment confirmation clears `expires_at` and sets status `CONFIRMED`.
- Cancellation clears `expires_at` and sets status `CANCELLED`.
- Application-side TTL: a Spring scheduler runs every minute to cancel `PENDING_PAYMENT` bookings whose `expires_at <= now`, logs a `unit_events` row, and adjusts the availability cache.
- Availability cache is kept in sync via create/cancel/payment flows and the scheduler.

## Availability caching
- Redis-backed counter (`RedisAvailabilityCache`), lazy-initialized, with periodic refresh and DB fallback.
- Cache is updated on unit creation and booking status changes; can recover after crashes by refreshing from DB.
- Endpoint `GET /api/v1/stats/availability` returns the cached count.

## Key endpoints
- `POST /api/v1/units` — create a unit with rooms/type/floor/description/baseCost.
- `GET /api/v1/units` — search by rooms, type, floor, minCost/maxCost (with markup), date range, pagination, and sorting.
- `POST /api/v1/bookings` — create booking (15-minute payment window).
- `POST /api/v1/bookings/{id}/pay` — confirm payment.
- `POST /api/v1/bookings/{id}/cancel` — cancel booking.
- `GET /api/v1/stats/availability` — availability metric from cache.

## Data and schema (Liquibase)
- Single SQL changelog (`db/changelog/changes/001-init.sql`) creates tables and seeds data:
  - 2 users, 10 fixed units with creation events.
  - 90 additional deterministic units with creation events.
- `002-add-booking-expiry.sql` adds `expires_at` to bookings.
- Sequences are advanced to avoid ID collisions with seeded rows.

## Tests
Run:
```bash
GRADLE_USER_HOME=./.gradle ./gradlew test
```
Tests cover services, controllers, Redis cache integration, and booking/unit flows with Spring Test and Mockito. Docker is required for the Redis Testcontainers integration test.
