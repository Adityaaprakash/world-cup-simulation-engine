# Phase 10 Integration Points

## Match-Engine Integration
- Phase 10 additions must weave seamlessly via `MatchModifierService.java`. Currently, modifiers logically scale base team strength against momentum elements. The new fatigue architectures must systematically degrade specific sub-stats (e.g. pace dynamically falling as fatigue reaches 80% during simulations).
- **Match Cycle Tracking (Phase 10B)**: `PlayerStateService.java` parses `events` globally post-match intercepting precise minutes. Duplicate applications are blocked explicitly by only recording calculations inside the terminal `updateAfterMatch` lifecycle, preserving purely transient mutation maps protecting `Player` base metrics natively.

## Player Availability Integration
- `PlayerStateService.java` already encapsulates availability checks (`isAvailable`). Phase 10 will deepen this logic by incorporating contract eligibility (red card vs. cup-tied suspensions). These components seamlessly govern `SquadPlayer` availability blocks prior to API submission.

## Squad and Lineup Integration
- Enhancements targeting advanced tactical substitutions map heavily mapped onto `SubstitutionDecisionService.java`. Future intelligence implementations will dictate exact substitution times triggered contextually (e.g. manager panic withdrawing defenders trailing heavily mapping toward offensive instructions).

## Manager Decision Integration
- `Manager.java` operations flow outward mapping `CoachingStyle` to tactical decisions. The new Board Confidence vectors will parse Match resolutions passively, generating event triggers internally stored inside the pre-existing `CareerHistory` entities.

## API Integration
- `ManagerController.java` currently issues profile read outputs. This will structurally require extensions for new interaction endpoints (e.g., `POST /api/managers/{id}/jobs/apply`).
- Broad JSON standardized serialization outputs natively support deep entity integration without modifying underlying error mapping behaviors (`GlobalExceptionHandler`).

## Frontend Integration
- React pages (`ManagerCareer`, `SquadManager`) will be retrofitted heavily intercepting these new JSON data arrays cleanly through Axios endpoints displaying context visually instead of redesigning established structures or generic components (`StatusBadge.jsx`, layouts).

## Event and Persistence Integration
- The newly proposed components (Objectives, Contracts, Jobs) must persist uniformly via `SaveGameController` mapping robust JSON exporting protocols dynamically capturing the state. Any excluded entities risk save deserialization corruption.

## Testing Integration
- Test cases within `WorldcupApplicationTests.java` are strictly initialized reflecting full context boots. Advanced tests validating engine determinants (e.g. validating an injured player genuinely cannot be rostered on starting XI selections) will safely be siloed across local `@SpringBootTest` boundaries targeting exact business logic functions.
