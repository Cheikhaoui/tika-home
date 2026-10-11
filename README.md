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
# 1. Database (PostgreSQL 17 + pgvector) on localhost:5433 and Mailpit (fake email inbox) on localhost:8025
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
Emails (local inbox, nothing is really sent): http://localhost:8025

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

## Calendar sync with Airbnb / Booking.com (iCal)

**Import:** every 15 minutes (`tikahome.ical.sync-interval`) the API downloads each enabled channel's
iCal feed and stores its reservations as blocked dates (`source = ICAL`). Cancelled reservations
disappear at the next sync. A failed download keeps the existing blocks.

Until the admin dashboard exists (phase 5), add a channel with SQL. Get the URL from
Airbnb (*Calendar → Availability → Connect calendars → Export*) or Booking.com
(*Rates & Availability → Sync calendars → Export*):

```bash
docker exec -it tikahome-postgres psql -U tikahome -d tikahome -c "
  insert into channel (id, property_id, type, name, import_url)
  values (gen_random_uuid(), '6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b', 'AIRBNB', 'Airbnb',
          'https://www.airbnb.com/calendar/ical/....ics?s=...');"
```

`type` is one of `AIRBNB`, `BOOKING_COM`, `VRBO`, `OTHER`. Check the result in the `channel` table
(`last_sync_status`, `last_error`, `last_conflict_count`).

**Export:** our calendar (active bookings + manual blocks, no personal data) is published at
`/ical/{token}.ics`. Get the URL with:

```bash
docker exec tikahome-postgres psql -U tikahome -d tikahome -tA -c \
  "select 'http://localhost:8081/ical/' || ical_export_token || '.ics' from property"
```

Paste it into Airbnb / Booking.com (*Import calendar*). The platforms must reach it over the internet,
so this only works once the API is deployed (phase 6); `localhost` is only for testing.

**Double bookings across platforms:** iCal is refreshed every few hours by the platforms, so a guest
can occasionally book the same dates elsewhere before the calendars catch up. The sync then imports
the reservation anyway (it is confirmed there) and logs a `DOUBLE BOOKING` warning plus
`last_conflict_count` on the channel. See the conflict policy in [CLAUDE.md](CLAUDE.md).

## Emails

| When | To | Language |
|---|---|---|
| Booking request | guest (reply goes to the owner) | guest’s language (FR / EN / AR, RTL) |
| Booking request | owner (reply goes to the guest) | English |
| Double booking detected by the sync | owner, once per conflict | English |
| A channel failed 3 syncs in a row / recovered | owner, once each | English |

Emails are written to the `email_outbox` table **in the same transaction** as the booking or sync,
then sent by a background job every 30 s with retries (1 min, 5 min, 30 min, 2 h, then `FAILED`).
A booking never fails because of email, and no email is lost if the mail server is down.

Locally, Mailpit (from `docker compose up -d`) catches everything at http://localhost:8025.
Settings live under `tikahome.notifications` in `application.yml` (owner address, owner language).
Texts: `backend/src/main/resources/i18n/emails*.properties`; templates: `templates/email/`.

## Tests

```bash
cd backend
./mvnw verify      # unit tests (Surefire) + *IT integration tests on a real Postgres (Failsafe + Testcontainers)
```

Docker must be running. Key tests:

- `BookingNoOverlapConstraintIT`: raw SQL inserts prove the **database** rejects overlapping bookings.
- `BookingApiIT`: end to end over HTTP, including 10 concurrent requests for the same dates → exactly one booking.
- `ChannelSyncIT`: iCal import against a fake Airbnb server (create / update / delete, outages, conflicts).
- `IcalExportIT`: the exported calendar parses back correctly and contains no personal data.
- `EmailOutboxIT` / `BookingEmailIT`: emails over a real SMTP test server (GreenMail), retries, dedupe.

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
