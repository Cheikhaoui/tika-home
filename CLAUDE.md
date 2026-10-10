# Tika Home — Direct Booking Website

## Context
Direct booking website for a rental house in Marrakech. The house will also be
listed on Airbnb, Booking.com and other platforms later, so **this site is the source of truth for
availability** and must sync with external channels (iCal) without ever allowing double bookings.

## Tech stack
- **Backend** (`/backend`): Spring Boot (latest stable), Java 25 LTS, Maven wrapper (`./mvnw`),
  Spring Data JPA, Flyway, Bean Validation, Spring Security (JWT, admin only — phase 5), springdoc-openapi
- **Database**: PostgreSQL (`pgvector/pgvector` image, pgvector used in phase 7)
- **Frontend** (`/web`): Next.js (App Router) + TypeScript + Tailwind CSS, built-in i18n (`app/[lang]`, `next/root-params`, JSON dictionaries) for FR / EN / AR (RTL for Arabic)
- **Local dev**: Docker Compose
- **Tests**: JUnit 5, Mockito, Testcontainers

## Repository layout
```
/backend            Spring Boot API
/web                Next.js site (public pages + /admin)
docker-compose.yml  Local PostgreSQL
CLAUDE.md, README.md
```

## Core domain
- `Property`, `Guest`, `Booking` (status: `REQUESTED`, `CONFIRMED`, `DECLINED`, `CANCELLED`),
  `BlockedDate` (source: `MANUAL`, `ICAL`), `PriceRule` (seasonal prices, min nights).
- **Critical rule — no double bookings, ever.** Enforced in two layers:
  1. Database: PostgreSQL exclusion constraint (`btree_gist`) on `daterange(check_in, check_out, '[)')`
     for active bookings (`REQUESTED`, `CONFIRMED`). Booking vs booking overlap is impossible.
  2. Service: every write that changes availability (booking request, admin block, iCal import) is
     `@Transactional` and first locks the property row (`PropertyRepository.findByIdForUpdate`), then
     checks bookings + blocked dates, then writes. Constraint violations (SQLSTATE `23P01`) map to HTTP 409.
- Blocked dates may overlap each other on purpose (several channels export the same dates), so they
  have no exclusion constraint; the property lock protects booking vs blocked-date races.
- `REQUESTED` bookings hold their dates; `DECLINED`/`CANCELLED` release them.
- `Channel` = an external platform (Airbnb, Booking.com, ...). Its iCal feed is imported every 15 min
  as `ICAL` blocked dates, diffed by event UID (`ChannelSyncService`): download outside any transaction,
  then apply under the property lock; a failed download keeps existing blocks.
- iCal export (`/ical/{token}.ics`): active bookings (REQUESTED + CONFIRMED) + MANUAL blocks, never
  ICAL blocks (no echo), never personal data. The token and channel import URLs are secrets: never log them.

## Double-booking conflict policy (cross-platform)
iCal lags by hours, so a platform reservation can overlap a direct booking. The software never
auto-cancels anything: it imports the platform reservation (it is confirmed and paid there), flags the
conflict (`DOUBLE BOOKING` log + `channel.last_conflict_count`) and alerts the owner (email in phase 3).
Recommended resolution shown to the owner (phase 5 admin):
- direct booking still `REQUESTED` -> decline ours politely with alternative dates (no platform penalty);
- direct booking `CONFIRMED` (it came first) -> first come, first served, but cancelling on Airbnb has
  penalties: owner relocates one guest to equivalent accommodation, or cancels on the platform.
- Before the owner confirms a request (phase 5), re-sync channels first to close the gap.

## Conventions
- Backend packages by layer under `com.tikahome`:
  `config`, `controller`, `dto` (API request/response records), `entity` (JPA entities + enums),
  `exception` (custom exceptions + `ApiExceptionHandler`), `model` (non-entity domain types such as
  `DateRange`, `Quote`, `Availability`), `repository`, `service`. Tests mirror these packages;
  Testcontainers integration tests (`*IT`) live in `integration`.
- Controllers only map HTTP <-> DTOs and call services; entities are never returned by controllers.
- Dates: `LocalDate`, half-open ranges `[checkIn, checkOut)`; property timezone `Africa/Casablanca`.
- Money: `BigDecimal` + ISO currency code; prices are always computed server-side.
- IDs: UUID. Errors: RFC 9457 `ProblemDetail`.
- Schema changes only through new Flyway migrations (never edit an applied one).
- Web: Next.js 16 differs from older versions (`proxy.ts` replaces `middleware.ts`, Cache Components on,
  root params). Before writing Next.js code, read the relevant guide in `web/node_modules/next/dist/docs/`
  (see `web/AGENTS.md`). Server Components by default; add `"use client"` only for interactivity.
  Translations live in `web/src/app/[lang]/dictionaries/*.json` (en.json defines the shape).
  Use logical Tailwind utilities (`ms-*`, `ps-*`, `start-*`) instead of left/right so RTL works.
- Small commits, Conventional Commit messages (`feat(backend): …`, `test: …`, `docs: …`).
- Don't build features from a later phase without asking.

## Roadmap
1. ✅ Project setup + backend core — entities, migrations, availability + quote + booking request API
2. ✅ Channel sync — iCal import (scheduled job, Airbnb / Booking.com / others) and iCal export endpoint
3. Email notifications (guest + owner, incl. double-booking alerts)  ← next
4. Public website — gallery, availability calendar, price calculator, booking form, WhatsApp button, FR/EN/AR, SEO
5. Admin dashboard — login (JWT), accept/decline bookings, block dates, manage prices
6. Deployment — Docker, GitHub Actions CI, cloud hosting (+ ShedLock for scheduled jobs)
7. AI guest assistant — Spring AI + RAG (pgvector) answering guest questions in their language
8. Later — online payments

## Commands
```bash
docker compose up -d                 # start PostgreSQL on localhost:5433
cd backend && ./mvnw verify          # build + all tests (needs Docker for Testcontainers)
cd backend && ./mvnw spring-boot:run # API on :8081, Swagger UI at /swagger-ui.html
cd web && npm run dev                # site on :3000
```
