# System Architecture

## Overview
The World Cup Simulation Engine operates as a secure, full-stack multi-tier application.

## 1. Backend Application (Spring Boot)
- **Framework**: Spring Boot 3 / Java 22.
- **Data Access**: Spring Data JPA utilizing Hibernate over PostgreSQL. Read actions bypass the N+1 problem through intelligent JPA projections and Pageable searches.
- **Security**: Stateless JWT filtering. All routes except `/api/auth/register`, `/api/auth/login`, and `/api/health` require non-expired tokens. Action endpoints are protected by `ROLE_USER`, and maintenance endpoints mandate `ROLE_ADMIN`.
- **Validation**: Jakarta validations enforcing database constraints at the payload edge, mapping to standard JSON response objects via `GlobalExceptionHandler`.
- **Engine Logic**: Domain objects enforce consistency. Match simulations run via transient contexts `MatchContext` and `TournamentContext` to prevent state saturation. `TacticalModifierService` and `MatchModifierService` evaluate events without destroying original player ratings.

## 2. Relational Database Interface (PostgreSQL)
- **Platform**: Hosted via Docker Compose (`postgres:16`).
- **Topology**: Actively mapped to `:5555:5432` locally to bypass native Windows constraints. Connection mandates `-Duser.timezone=UTC` on Windows hosts.
- **Migrations**: `Flyway` automatically manages schema updates dynamically from `classpath:db/migration`. 

## 3. Frontend Web Interface (React)
- **Framework**: React + Vite (Node v22).
- **Styling**: Tailwind CSS.
- **State**: External communication managed strictly through Axios REST interactions.
- **Routing**: Client-side single-page-application (SPA) architecture via React Router.

## 4. Integration Properties
To guarantee reproducibility and production hygiene, the infrastructure exposes all critical variables to environment contexts:
- Database details (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- Security rules (`JWT_SECRET`, `CORS_ALLOWED_ORIGINS`).
