# Tika Home

Direct booking website for a house in Marrakech.

| Folder    | What                                   | Stack                                          |
|-----------|----------------------------------------|------------------------------------------------|
| `backend` | Booking API                            | Spring Boot 4.1, Java 25, PostgreSQL, Flyway   |
| `web`     | Public website (FR / EN / AR) + admin  | Next.js 16 (App Router), TypeScript, Tailwind  |

See [CLAUDE.md](CLAUDE.md) for conventions and the roadmap.

## Prerequisites

- **JDK 25** (e.g. `brew install --cask temurin@25`). Maven is not needed: use `./mvnw`.
- **Node.js 20.9+** (24 LTS recommended)
- **Docker Desktop** (local database and Testcontainers)

## Run locally

```bash
# 1. Database (PostgreSQL 17 + pgvector) on localhost:5433
docker compose up -d

# 2. API on http://localhost:8081 (Flyway creates the schema and a seed property on startup)
cd backend
./mvnw spring-boot:run

# 3. Website on http://localhost:3000 (redirects to /fr, /en or /ar)
cd web
npm install
npm run dev
```

API docs: http://localhost:8081/swagger-ui.html

> Ports 5433 / 8081 are used (instead of 5432 / 8080) to avoid clashing with other local projects.

## Try the API

The seed property id is `6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b` (placeholder prices, in EUR).

```bash
P=6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b

# Availability ("to" is exclusive)
curl "localhost:8081/api/properties/$P/availability?from=2026-12-01&to=2026-12-31"

# Price quote (per-night breakdown, cleaning fee, total, issues)
curl "localhost:8081/api/properties/$P/quote?checkIn=2026-12-18&checkOut=2026-12-22&guests=4"

# Booking request -> 201 REQUESTED; send it twice -> 409 Conflict
curl -X POST localhost:8081/api/bookings -H 'Content-Type: application/json' -d '{
  "propertyId": "'$P'", "checkIn": "2026-12-18", "checkOut": "2026-12-22", "guests": 4,
  "guest": { "fullName": "Jane Doe", "email": "jane@example.com", "language": "en" },
  "message": "Arriving late"
}'
```

| Status | Meaning                                                                     |
|--------|-----------------------------------------------------------------------------|
| 201    | Booking created with status `REQUESTED`                                     |
| 400    | Invalid input; `errors` lists each invalid field                            |
| 409    | Dates not available (`issues: ["DATES_UNAVAILABLE"]`)                       |
| 422    | Business rule broken, e.g. `BELOW_MIN_NIGHTS`, `TOO_MANY_GUESTS`            |

Errors use [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details.

## Tests

```bash
cd backend
./mvnw verify      # unit tests (Surefire) + *IT integration tests on a real Postgres (Failsafe + Testcontainers)
```

Docker must be running. Key tests:

- `BookingNoOverlapConstraintIT`: raw SQL inserts prove the **database** rejects overlapping bookings.
- `BookingApiIT`: end to end over HTTP, including 10 concurrent requests for the same dates → exactly one booking.

```bash
cd web
npm run lint && npm run build
```

## How double bookings are prevented

1. **Database:** an exclusion constraint on `booking`
   (`EXCLUDE USING gist (property_id WITH =, daterange(check_in, check_out, '[)') WITH &&)`, for
   `REQUESTED`/`CONFIRMED` only) makes it impossible to store two overlapping active bookings, even under
   concurrency. See `backend/src/main/resources/db/migration/V2__booking_no_overlap.sql`.
2. **Service:** `BookingService.request` locks the property row (`SELECT … FOR UPDATE`), re-checks
   availability, blocked dates and rules, then inserts. Concurrent requests are serialized and get a clean 409.

Stays are half-open `[checkIn, checkOut)`: a guest can check in on the day another checks out.

## Reset the local database

```bash
docker compose down -v && docker compose up -d
```
