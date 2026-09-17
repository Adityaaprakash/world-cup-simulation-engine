# Changelog

All notable changes to the World Cup Simulation Engine are documented here. The system operates on a phase-based rollout mapping core structures to historical completions.

## [Phase 10] - Advanced Simulation & Manager Ecosystem
### Phase 10M: Cloud Delivery Infrastructure & CI/CD Readiness
- **Added**: Full-stack GitHub Actions pipeline (`ci.yml`) validating Java tests dynamically alongside standard Node JS frontend mapping parameters. No explicit deployments are scheduled statically.
- **Added**: Nginx fallback architectures explicitly resolving internal Vue/React path dependencies directly off `/dist` structures via `frontend/Dockerfile`.
- **Added**: Dedicated `backend` Spring Boot `Dockerfile` routing exclusively via non-root Temurin distributions maximizing security layers.
- **Added**: Complete `Helm` capabilities located at `deploy/helm/worldcup/` natively bridging config maps alongside Base64 encoded Kubernetes Secrets isolated securely off volatile configurations.
- **Changed**: Improved root `docker-compose.yml` natively encapsulating isolated bridges interconnecting backend and UI seamlessly beside established Postgres 16 integrations.

### Phase 10K: Frontend Knockout Tree Visualization & Penalty Telemetry
- **Added**: Real-time integration supporting Extra time (`AET`) and Penalty markers (`Pens`) directly within `MatchHeader.jsx` components scaling gracefully between Live STOMP streams and standard REST endpoints without layout fractures.
- **Added**: `KnockoutBracketResponse` JSON mappings into native React DOM structural arrays within `KnockoutBracket.jsx` dynamically projecting frontend hierarchies without manipulating backend logic workflows.
- **Changed**: Reconfigured `tournamentApi.js` seamlessly switching from legacy flat REST sequences to explicit `/api/tournaments/{tournamentId}/knockout/bracket` endpoints.

### Phase 10J: Tournament Operations & Competition Management (A, B & C)
- **Added**: Built-in deterministic generation for Extra Time and Penalty Shootouts resolving drawn knockout parameters directly within `MatchSimulationService` via `MatchContext` bound updates mapping properly into updated Entity domains natively backed by `V31__add_extra_time_penalties_to_matches.sql` (10J-A).
- **Added**: Sequential Knockout mapping (`KnockoutSimulationService` traversing Ro16 through Final) algorithmically scheduling next-round fixtures and resolving bracket progression tied definitively to winner decisions incorporating penalty shootouts (10J-B).
- **Added**: Native `KnockoutBracketResponse` hierarchically structured endpoint dynamically exposed at `GET /api/tournaments/{id}/knockout/bracket` safely decoupling bracket telemetry UI integrations (10J-B).
- **Changed**: Replaced the ambiguous `IN_PROGRESS` tournament lifecycle enum explicitly with separated `GROUP_STAGE` and `KNOCKOUT_STAGE` domains natively spanning backend dashboards mapped via idempotent Flyway migration `V32__split_tournament_status.sql` handling pre-existing records transparently (10J-C).

### Phase 10I: Live Match Experience (A, B, C, D & E)
- **Added**: `WebSocketConfig` mapping `/ws` endpoints supporting decoupled unauthenticated STOMP infrastructure scaling safely across REST contexts.
- **Added**: `LiveMatchBroadcasterService` scheduling sequences natively tracking progressive arrays tracking Match timelines seamlessly to active broadcast mappings mapping real-time simulation experiences safely.
- **Added**: Live broadcaster integration in `TournamentMatchSimulationService` mapping transaction hooks to broadcast exactly once after the database safely commits the final timeline. Broadcast errors gracefully fail without disrupting the REST response lifecycle.
- **Added**: `useLiveMatch` React Hook seamlessly handling deterministic SockJS endpoints decoupling native match payloads deduplicated against active disconnected fallbacks elegantly replacing static timelines dynamically securely via Vitest.
- **Added**: `WebSocketAuthenticationInterceptor` ensuring WebSocket connections dynamically authenticate over STOMP `CONNECT` protocols reusing native REST JWT algorithms safely decoupling sensitive sessions strictly masking token prints effectively.
- **Improved**: Complete Live Event deduplication pipelines natively binding granular properties safely preventing identical sequences colliding explicitly. Broadcaster schedulers strictly intercept failed lambda handlers securely mapping completion callbacks precisely avoiding dangling threads accurately.

### Phase 10H: Manager Job & Board Confidence System
- **Added**: Continuous timeline algorithms dynamically mapping Manager objectives natively evaluating confidence metrics post-match structurally sacking active managers mapping dynamic bounds.

### Phase 10C: Injury & Recovery System
- **Added**: Deterministic Injury mappings restricting injured players from entering the starting XI and enforcing instant mid-match substitutions synchronously when sustaining in-game injuries. Includes deterministic recovery loops scaling match progression natively.

### Phase 10B: Player Form & Fatigue Engine
- **Added**: Real-time deterministic mapping parsing Match Events dynamically mapping exact minutes played mathematically resolving Form boundaries `[-10, 10]` and `Fatigue` ceilings `[0, 100]`. Full integration without mutating base `Player` attributes.

### Phase 10A: Architecture & Domain Audit
- **Added**: Comprehensive domain audit and architectural blueprints (`PHASE_10_ARCHITECTURE.md`, `PHASE_10_DOMAIN_MODEL.md`, `PHASE_10_INTEGRATION_POINTS.md`) for building advanced Player Forms, Manager Objectives, Contracts, and Fatigue logic atop the tested Phase 9 foundations natively without modifying schemas or application tests.
## [Phase 9] - Historical Intelligence & Ecosystem Polish

### Phase 9K-3: Historical Intelligence
- **Added**: `/api/history` ecosystem supplying intelligent Hall of Fame generation, era evaluations, global rankings, and tournament timelines. Cached to prevent heavy simulation loading.

### Phase 9K-1: Advanced Search Mechanics
- **Added**: Complex structured, pageable backend dataset filtering (e.g., `/api/search/players`) yielding dynamic subsets of database entries using Spring Data Specifications.

### Phase 9J-3: Administrator Operations Framework
- **Added**: Operational tooling for cache truncation/rebuilding, orphan save cleaning, and generalized database metrics.
- **Added**: Read-only maintenance query logs via `admin_audit_logs`.

### Phase 9J-2: Secure Football Dataset APIs
- **Added**: Administrator configurations covering player lifecycle updates (retirements, bulk modifications) and global team validation.

### Phase 9I-1 & 9I-2: Career Management Systems
- **Added**: Structured save metadata exporting and importing across REST endpoints.
- **Added**: Persistent save slots, intelligent background autosaves overlaying manager profiles dynamically.

### Phase 9H-1 & 9H-2: Manager Ecosystem 
- **Added**: Automated tracker for XP allocations, badges, accolades, leaderboards, and historical timeline events linked intrinsically to matching generation logic.

### Earlier Milestones (Phase 9F - 9G): Performance and API Standards
- **Added**: Benchmark runners mapping xG and backend engine durations per match iteration.
- **Changed**: Widespread REST unification via common standard error schema returns for consistency across API clients.

## Final Environment Audit (Phase 9M-5 / 9M-6)
- **Changed**: Port binding shifted gracefully to 5555 to mitigate host collision failures.
- **Changed**: Removed tracked loose `_errors.txt` log artifacts for spotless hygiene.
- **Added**: Release readiness documentation.
