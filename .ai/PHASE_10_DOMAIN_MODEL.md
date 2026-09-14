# Phase 10 Domain Model Audit

## Existing Entities & Responsibilities
- **`Player`**: Stores immutable base attributes (`overallRating`, `potential`, etc.). Validated source of truth mapping player identity to country structures.
- **`PlayerState`**: Extends `Player` exclusively handling transient modifiers. Phase 10B implemented exact bounded structures dynamically across Form `[-10, 10]` and Fatigue `[0, 100]`. Phase 10C implemented precise deterministic `InjuryStatus` bounds (MINOR: 1 match, MODERATE: 3 matches, MAJOR: 5 matches) restricting player availability flawlessly.
- **`SquadPlayer`**: Acts as a junction table tying a `Player` sequentially to a tournament `Squad`.
- **`Manager`**: Houses human user state mappings (reputation, coaching style).

## Proposed Future Entities (Phase 10)
1. **`PlayerContract`**
   - **Responsibility**: Link a `Player` natively to a Club/National identity covering duration blocks (seasons/tournaments), wages (if financial contexts apply), and squad roles.
2. **`TransferEvent`**
   - **Responsibility**: Historical record detailing player movement timelines between domains safely separated from mutable `Player.country` assignments.
3. **`ManagerJob`**
   - **Responsibility**: Links `Manager` natively to active management contracts indicating start date, confidence scores, and objectives.
4. **`BoardObjective`**
   - **Responsibility**: Maps dynamic expectations (e.g. "Reach Semi-Finals", "Win Tournament") generating pass/fail states triggering confidence cascades.

## Relationships & Ownership Boundaries
- `Player` records remain globally distinct. Ownership transitions exclusively occur through `PlayerContract` validations. 
- Transient forms of tracking (e.g., historical match performance) should NOT live inside `PlayerState` to avoid bloat; they will continue to populate via `MatchEvents` and `MatchStatistics`.

## Fields That Cannot Be Duplicated
- Base mathematical ratings (Pace, Shooting, etc.) must NEVER be structurally replicated inside `PlayerState` or external modules. All match-resolution evaluations must inject transient alterations virtually over the base models during execution logic.

## Migration Considerations
- Current `Manager.java` does not require club association natively yet as national alignments define international football frameworks. However, standard league adaptations will require migrating existing managers automatically to dummy or default unstructured jobs gracefully via Flyway.

## Open Design Questions
1. **Transfer Frequency**: Specifically evaluating whether `TransferEvent` triggers daily internally via offline events or strictly upon simulation milestones (e.g. at the conclusion of a tournament epoch).
