# Phase 10 Architecture and Integration Audit

## Phase 10 Goals
The primary goal of Phase 10 is to heavily expand the World Cup Simulation Engine by transitioning from simple transient match states to a deeply connected career ecosystem focusing on:
- Player form, fatigue, training, and recovery.
- Transfers and squad registrations.
- Manager movement, board objectives, and career longevity.
- Sophisticated tactical management, including intelligent substitution dynamics integrated deeply with fatigue contexts.

## Existing Architecture Findings relevant to Phase 10

### 1. Player Domain Findings
- The application currently implements `Player.java` containing immutable definitions (Rating, Pace, Position) and `PlayerState.java` containing mutable conditions (`currentForm`, `confidence`, `fitness`, `fatigue`, `morale`, `yellowCards`, `injuryStatus`).
- **Reuse**: The existing `PlayerState` is very robust serving as a mutable 1-to-1 extension of `Player` meaning Phase 10 form and fatigue engines should directly manipulate `PlayerState` properties rather than rebuilding new entities in parallel. 
- **Gaps**: There is no historical tracking of form/fatigue mapping (e.g. tracking historical injuries), nor is there a `Contract` or `Transfer` registry.

### 2. Squad and Lineup Findings
- Squad lists are maintained via `SquadPlayer.java` mapping `Player` natively to `Squad.java`. 
- **Gaps**: Squad size validation and squad registration restrictions (like registering 26 players per specific season vs dynamic active rosters) are not present.

### 3. Match Engine Findings
- Match resolution invokes `PlayerStateService.updateAfterMatch` tracking simple integer manipulation over `PlayerState` fields, decaying form or resting benched players logically.
- `SubstitutionDecisionService.java` currently manages substitution routines by evaluating positional configurations, fatigue, bookings, and tactical fitness.
- **Integration Points**: Phase 10 form logic must directly interface inside `SubstitutionDecisionService` (e.g., pulling heavily fatigued players early) and `MatchModifierService` preventing permanent mutation of `Player` overall ratings while applying deep tactical effects organically.

### 4. Manager Career Findings
- `Manager.java` stores profile identities. `CareerHistory.java`, `ManagerAchievement.java`, and `CareerTimelineEvent.java` provide comprehensive tracking elements.
- **Gaps**: Currently lacks `BoardObjective.java`, `ManagerJob.java`, and `Club.java` abstractions reflecting employment status or pressure.

### 5. Database Findings
- Database handles Flyway implementations gracefully natively (`V1` - `V28`).
- Required extensions: New tables mapping `contracts`, `transfers`, and `board_objectives` will cleanly implement atop the unified structure. Existing constraints will not be fractured.

### 6. API and Frontend Findings
- Endpoints logically siloed (e.g., `ManagerController`, `PlayerController`, `SquadController`).
- Phase 10 will expose advanced paths (`/api/managers/{id}/jobs`, `/api/squads/{id}/transfers`).

### 7. Testing Findings
- Integration logic primarily lives via `WorldcupApplicationTests.java` testing complete application context boot sequences.
- Need structured unit tests modeling the exact deterministic deterioration of Player Fatigue configurations before full API tests can be deployed.

## Proposed Phase 10 Component Boundaries
1. **Fatigue & Form Engine**: An isolated service operating alongside `MatchSimulationService` purely consuming Match Minutes per player and translating to `PlayerState` mutations safely.
2. **Transfer Engine**: Managing global squad mutations dynamically resolving logic asynchronously between simulation milestones.
3. **Manager Jobs Context**: Overlaying the historical simulation sequences by associating the `Manager` dynamically across entities based on reputation limits.

## Explicit Non-Goals for Phase 10A
- No feature logic implementation.
- No database migrations or native structural modifications.
- No frontend React additions.
