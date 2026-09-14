# Changelog

All notable changes to the World Cup Simulation Engine are documented here. The system operates on a phase-based rollout mapping core structures to historical completions.

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
