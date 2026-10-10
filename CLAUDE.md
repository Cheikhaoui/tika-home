# Tika Home — Direct Booking Website

## Context
Direct booking website for a rental house in Marrakech (owned by a friend). The house will also be
listed on Airbnb, Booking.com and other platforms later, so **this site is the source of truth for
availability** and must sync with external channels (iCal) without ever allowing double bookings.

This is also a learning project: the developer is a Java/Spring Boot + Angular engineer (4 years)
learning React/Next.js and AI integration (Spring AI), aiming at AI engineering roles (NL/BE).
When working here, act as a senior engineer **and** mentor: briefly explain key decisions,
especially React/Next.js specifics and anything related to double-booking prevention.

## Tech stack
- **Backend** (`/backend`): Spring Boot (latest stable), Java 25 LTS, Maven wrapper (`./mvnw`),
  Spring Data JPA, Flyway, Bean Validation, Spring Security (JWT, admin only — phase 5), springdoc-openapi
- **Database**: PostgreSQL (`pgvector/pgvector` image, pgvector used in phase 7)
- **Frontend** (`/web`): Next.js (App Router) + TypeScript + Tailwind CSS, next-intl for FR / EN / AR (RTL for Arabic)
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
  1. Database: PostgreSQL exclusion constraints (`btree_gist`) on `daterange(check_in, check_out, '[)')`
     for active bookings (`REQUESTED`, `CONFIRMED`) and for blocked dates.
  2. Service: `@Transactional` creation that locks the property row, checks bookings + blocked dates,
     then inserts. Constraint violations (SQLSTATE `23P01`) map to HTTP 409.
- `REQUESTED` bookings hold their dates; `DECLINED`/`CANCELLED` release them.

## Conventions
- Backend packages by feature: `com.tikahome.<feature>` (`property`, `booking`, `availability`,
  `pricing`, `guest`, `common`). DTOs are Java `record`s; entities never leave the service layer.
- Dates: `LocalDate`, half-open ranges `[checkIn, checkOut)`; property timezone `Africa/Casablanca`.
- Money: `BigDecimal` + ISO currency code; prices are always computed server-side.
- IDs: UUID. Errors: RFC 9457 `ProblemDetail`.
- Schema changes only through new Flyway migrations (never edit an applied one).
- Small commits, Conventional Commit messages (`feat(backend): …`, `test: …`, `docs: …`).
- Don't build features from a later phase without asking.

## Roadmap
1. **Project setup + backend core** — entities, migrations, availability + quote + booking request API  ← current
2. Channel sync — iCal import (scheduled job, Airbnb / Booking.com / others) and iCal export endpoint
3. Email notifications (guest + owner)
4. Public website — gallery, availability calendar, price calculator, booking form, WhatsApp button, FR/EN/AR, SEO
5. Admin dashboard — login (JWT), accept/decline bookings, block dates, manage prices
6. Deployment — Docker, GitHub Actions CI, cloud hosting
7. AI guest assistant — Spring AI + RAG (pgvector) answering guest questions in their language
8. Later — online payments

## Commands
```bash
docker compose up -d                 # start PostgreSQL
cd backend && ./mvnw verify          # build + all tests (needs Docker for Testcontainers)
cd backend && ./mvnw spring-boot:run # API on :8080, Swagger UI at /swagger-ui.html
cd web && npm run dev                # site on :3000
```
