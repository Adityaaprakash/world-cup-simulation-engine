# World Cup Simulation Engine

## Dynamic player state

Each player has a persisted state separate from their base `overall_rating`. New
states begin with form `0`, confidence and morale `50`, fitness `100`, fatigue
`0`, no cards or suspension, and a healthy injury status. The base rating is
never updated by the simulation.

After a simulated match, starting players (and recorded substitutes) lose a
small amount of fitness and gain fatigue. Non-playing squad members recover
fitness and reduce fatigue. Winners gain morale and confidence, while losers
lose morale. Goals increase the scorer's confidence and form, assists improve
form, clean sheets improve the starting goalkeeper's confidence, and cards are
recorded. A red card creates a one-match suspension.

At the end of each participating team's matchday, existing suspensions and
injury recovery counters decrease by one. An injury becomes `HEALTHY` once its
counter reaches zero. Form also moves one point toward zero after each
matchday.

Simulation strength uses an effective rating calculated from the immutable
base rating plus small form, confidence, fitness, fatigue, and morale effects.
Suspended players and players with active moderate or major injuries are
unavailable for event selection and contribute no effective strength.

## Advanced tactical engine

Every team has one persisted tactical profile. Existing teams receive a
balanced default profile, so no configuration is required before simulating.
The profile includes attack and defensive width, defensive line, pressing,
build-up style, chance creation, attacking width, crosses, long balls, passing
risk, counter attacks, high press, offside trap, and time wasting.

Tactical profiles do not change base team or player ratings. Instead,
`TacticalModifierService` derives possession, attack, defense, counter, press,
fatigue, discipline, passing, crossing, and offside modifiers for each match.
Those modifiers flow through scoreline selection, expected goals, possession,
shots, pass accuracy, fouls, cards, offsides, assists, and player-state
fatigue/recovery.

High pressing raises attacking pressure and card/fatigue risk. A high defensive
line supports offside traps but improves the opponent's counter-attack modifier;
a low line is less exposed to counters. Slow possession improves ball control
and pass completion while reducing attacking urgency, while direct play trades
possession for faster attacks. Wide and crossing-oriented tactics create more
assisting opportunities; high-risk passing generates more attacking upside at a
pass-completion cost. Tactical profiles can be updated through
`TacticalProfileService.updateProfile` using `TacticalProfileUpdateRequest`.

## AI manager

The backend AI manager prepares every simulated squad before kickoff. It selects
the closest suitable formation from the configured formations, then builds an
availability-aware starting XI from the registered squad. Selection favours
effective rating, current form, confidence, morale, and fitness, while excluding
suspended and unavailable injured players. High fatigue, low fitness, and the
more rotation-prone wide positions lower selection priority; goalkeepers rotate
less aggressively.

`PlayerEvaluationService` provides the common AI score used by lineup, bench,
captain, and substitution decisions. It combines effective rating, base quality,
form, confidence, morale, fitness, fatigue, position fit, experience, and
availability without changing the stored base rating.

The manager exposes reusable backend methods for selecting a match squad,
starting eleven, bench, formation, captain, tactical profile, substitutions,
rotation checks, and player evaluation. The bench selector keeps positional
balance by preferring a goalkeeper, defenders, midfielders, and attackers
instead of simply taking the next best attackers.

Formation choice uses existing configured formations. Winger-heavy squads
prefer 4-3-3 shapes, strong central-midfield squads prefer 4-2-3-1-style shapes,
strong striker groups prefer 4-4-2-style shapes, and heavy underdogs prefer a
five-defender shape when available. Name matching is used only as a tie-breaker
among compatible formations.

Tactical selection starts from opponent strength and then adjusts to squad
traits. Fast attackers push the team toward direct counter attacks, creative
midfields toward possession, strong defenses toward a higher line and offside
trap, weak defenses toward a lower block, and high fitness toward pressing.
Poor stamina reduces pressing intensity.

After the simulated scoreline is known, tactical risk is adjusted for the match
state. The manager then replaces the generic substitutions with AI decisions at
60, 70, and 80 minutes. Leading teams trade attackers for fresh defenders or
midfielders, drawing teams make fitness-led balanced changes, and losing teams
introduce attackers while increasing press and attacking risk.

Match importance is derived internally from the match round. Group-stage matches
allow more rotation to protect tired players, knockout matches favour stronger
available lineups, semi finals rotate only when needed, and finals select the
strongest fit team. Recovered and suspension-cleared players automatically
return to the available pool because availability is recalculated before every
match.

In-match AI now reacts to red cards and extra-time pressure. Teams leading a
match reduce pressing, attacking width, and passing risk while enabling time
wasting. Trailing teams increase press and risk. A team with a red card lowers
its defensive line and pressing, while a team facing ten players increases
attacking pressure. Tied knockout matches are treated as extra-time contexts:
the AI can use an additional substitution, prefers fresher bench players, and
backs away from high pressing when stamina is low.

Substitution decisions protect booked players, players close to suspension, and
exhausted players while preserving captains when possible. Replacement choice
prefers tactical fit and same-line positional cover, then adapts to the score:
more defensive while leading, more attacking while trailing, and balanced while
drawing.

## Dynamic match engine

Each simulation now creates a transient `MatchContext` that is never persisted.
It tracks home and away momentum, pressure, attacking intent, defensive
confidence, match intensity, current phase, consecutive attacks, extra-time
state, and weather. Existing public simulation APIs still work; older calls use
balanced defaults.

Matches progress through phases: opening, settled play, end of first half,
restart, substitution phase, closing phase, extra time, and penalty shootout.
The active phase affects pressure, tempo, late knockout aggression, and
extra-time conservatism.

Events now chain into later events. Goals and assists increase momentum,
weaken the opponent's defensive confidence, and shift pressure. Red cards
reduce the sent-off team's control and defensive confidence while giving the
opponent more momentum. Yellow cards reduce aggression slightly and remain a
higher AI substitution priority. Substitutions reduce pressure by adding fresh
legs.

`MatchModifierService` applies momentum, pressure, game state, weather, home
advantage, fatigue, and tactical context to match modifiers. Home advantage is
small: an initial momentum nudge and slightly different pressure, not a hidden
rating boost. Rain and snow increase mistakes and reduce passing quality, hot
weather accelerates fatigue and lowers pressing, and clear weather is neutral.

Context-adjusted tactical modifiers flow into scoreline selection, event
generation, statistics, AI tactical reactions, substitutions, and player-state
fatigue updates. Tied knockout matches use an extra-time context with lower
tempo, higher fatigue, and more value placed on fresh substitutes.

Knockout draws are resolved through a lightweight penalty shootout model rather
than a random coin flip. Penalty order favours available outfield players with
shooting quality, confidence, lower fatigue, and overall ability. Shootout
probability accounts for goalkeeper confidence, shooter confidence, fatigue,
pressure, home calm, weather, and sudden death.

## Tournament intelligence

Tournament intelligence rebuilds a transient `TournamentContext` from persisted
fixtures, results, events, ratings, and statistics. The context is not stored in
the database. It tracks the current stage, remaining fixtures, group or knockout
state, favourites, dark horses, biggest upset, highest scoring team, best
defensive team, streak leaders, team form, reputation, and tournament momentum.

Team form is calculated separately from player form as a compact recent result
string such as `WWDWL`. It feeds tournament momentum, which then flows into the
dynamic match context before each tournament match. Upset wins, winning streaks,
large victories, and knockout progress increase momentum; heavy defeats and
loss streaks reduce it. Strong momentum gives a small future confidence lift
through the existing player-state system.

Teams are classified by reputation from tournament strength: favourite,
contender, outsider, or underdog. Outsiders and underdogs with strong momentum
or unbeaten runs become dark horses. Knockout matches add stage pressure,
especially in quarterfinals, semifinals, finals, and shootouts.

`MatchNarrativeService` produces deterministic narratives from saved match
statistics and events, such as possession dominance without conversion, late
pressure, clean-sheet control, high-scoring thrillers, and major upsets.

Existing player awards continue to provide Golden Boot, Golden Ball, Golden
Glove, Best Young Player, and Team of the Tournament. `TournamentTeamAwardsService`
adds Best Attack, Best Defence, and Fair Play from persisted scores and match
statistics.

`TournamentSummaryService` assembles tournament summaries including biggest
upset, most entertaining match, highest scoring match, top scorer, best
goalkeeper, champion path, longest streak, total goals, completed matches,
match narratives, and team awards.

## Performance and analytics

Phase 9F adds backend-only optimization services without changing simulation
APIs. `SimulationMetricsService` records completed match simulations, runtime
statistics, goals, cards, substitutions, possession, xG, extra-time frequency,
and penalty shootout frequency. Group-stage, knockout, and tournament-match
simulation services also record operation timings.

`BenchmarkService` can simulate configurable numbers of standalone matches and
tournaments. Reports include average, median, minimum, and maximum runtime,
goal distribution, average scoreline, and home win, away win, and draw
percentages.

`AnalyticsService` reports formation usage, formation win rate, average goals,
average possession, shots, corners, cards, clean-sheet percentage, and
tournament averages. `LeaderboardService` reports top scorers, assists, clean
sheets, highest rated players, best attacking and defensive teams, and yellow
and red card leaders.

Analytics, leaderboards, tournament summaries, team awards, and tournament
awards use short-lived in-memory caching controlled by
`simulation.optimization.cache-duration-ms` and
`simulation.optimization.analytics-cache-duration-ms`. Active simulations are
not cached.

## API hardening and production readiness

Phase 9G standardizes REST error handling and validation across the backend.
Validation failures, business conflicts, authentication failures, and access
denials return a consistent JSON error payload with `timestamp`, `status`,
`error`, `message`, and `path`. Request DTOs and path variables use Jakarta
Validation for required values, positive identifiers, bounded text, valid
scores, lineup size, and supported position slots.

Security remains stateless JWT-based authentication. Public routes are limited
to authentication, health checks, and OpenAPI documentation. Application APIs
remain authenticated by default, and operational optimization endpoints such as
metrics, benchmark execution, and cache clearing require the existing `ADMIN`
role.

OpenAPI documentation is exposed through springdoc at `/v3/api-docs` and
Swagger UI at `/swagger-ui.html`. Controllers include tags, endpoint summaries,
response descriptions, and parameter descriptions so clients can discover the
existing API without changing endpoint URLs.

Existing list endpoints keep their legacy array responses for backward
compatibility. Additive pageable endpoints are available for large read
collections at `/api/tournaments/page`, `/api/teams/page`, and
`/api/players/page`, supporting Spring `Pageable` and sort query parameters.

Lifecycle validation prevents duplicate tournaments for the same name and year,
group or fixture generation after a tournament has started, knockout generation
before all group-stage matches are complete, replaying completed matches, and
simulating completed tournaments. The health endpoint at `/api/health` returns
a lightweight status payload, while actuator health exposure is configured for
non-sensitive health/info access.

## Manager career core

Phase 9H-1 adds persistent manager careers without changing tournament
simulation inputs. A manager career is created lazily for the authenticated
user when `/api/managers/me`, `/api/managers/me/statistics`, or
`/api/managers/me/history` is requested, or when that user's squad manages a
completed tournament match. The manager profile stores username, display name,
nationality, favourite formation, favourite tactical profile, coaching style,
reputation, experience points, level, and timestamps.

Career statistics are persisted separately from the manager profile. They track
tournaments managed, matches managed, wins, draws, losses, goals scored, goals
conceded, clean sheets, trophies won, finals reached, and semifinals reached.
Manual match completion and simulated tournament matches update match-level
career statistics through the existing completion flow. Knockout match career
stats are recorded after existing penalty/shootout resolution so the stored
result matches the persisted score.

Career history is written when a tournament is completed. Each managed team
entry records the tournament, team, finishing position, wins, losses, goals
scored, goals conceded, trophies, and completion date. Duplicate history rows
for the same manager, tournament, and team are prevented.

Manager progression is handled by `CareerProgressionService`. Managers gain
configurable experience for match wins, draws, reaching the knockout phase,
reaching a final, winning a tournament, and future award hooks. Level is derived
from total experience, and reputation automatically advances through
`AMATEUR`, `PROFESSIONAL`, `ELITE`, `WORLD_CLASS`, and `LEGENDARY` according to
level. XP values are configured under `manager.progression` in
`application.yaml`.

Phase 9H-2 adds persistent achievements, badges, analytics, timeline events,
and manager leaderboards. Achievements are unlocked automatically after career
matches and completed tournaments. Examples include First Victory, First
Trophy, World Champion, Invincible Tournament, Defensive Master, Attacking
Genius, Penalty Specialist, Giant Killer, Clean Sheet Machine, Youth Developer,
and Tournament Veteran. Each achievement carries a badge tier from Bronze,
Silver, Gold, Platinum, or Diamond.

Career analytics are recalculated from existing persisted data instead of a
separate simulation pipeline. The analytics snapshot includes win percentage,
average goals scored and conceded, average possession from match statistics,
favourite formation from squads, favourite tactical style from tactical
profiles, inferred manager tactical profile, current most-used lineup, selected
captain, trusted starters, and longest unbeaten streak.

The manager timeline records chronological career events such as completed
tournaments, trophies, level promotions, reputation upgrades, achievements, and
milestones. Leaderboards expose the top managers by win rate, trophies, matches
managed, unbeaten streak, and reputation. The additional career endpoints are
`/api/managers/me/achievements`, `/api/managers/me/analytics`,
`/api/managers/me/timeline`, and `/api/managers/leaderboards`.

## Career save system

Phase 9I-1 adds persistent save slots for manager career metadata. Save slots do
not serialize tournaments or replay simulation state. Instead, each slot stores
metadata for the authenticated manager, including slot name and number, save
type, current tournament reference, season, stage, total play time, manager
level, reputation, tournaments played, trophies, current team, current
tournament, progress percentage, latest save timestamp, and active/autosave
flags.

Manual saves use positive slot numbers and are unique per manager. Slot `0` is
reserved for the manager's autosave, and autosave always overwrites that single
autosave slot. Save ownership is validated through the manager linked to the
authenticated user, tournament references are checked before saving, and active
autosaves cannot be deleted accidentally.

Autosaves are refreshed after completed tournaments, trophy wins, and major
achievement unlocks. The save APIs are:

- `GET /api/saves`
- `GET /api/saves/{id}`
- `POST /api/saves`
- `PUT /api/saves/{id}`
- `DELETE /api/saves/{id}`
- `POST /api/saves/autosave`
- `POST /api/saves/{id}/activate`

Phase 9I-2 extends save slots with a versioned resume/export/import workflow.
Each save now records the current save format version, manager experience
points, and latest backup metadata. Manual overwrites and autosave overwrites
refresh backup metadata before replacing slot metadata, and backups can also be
marked explicitly through `POST /api/saves/{id}/backup`.

Resume is handled through `POST /api/saves/{id}/resume`. Resuming validates save
ownership, save format compatibility, and the current tournament reference,
then restores the manager's saved progression metadata and activates the slot.
Tournament stage, squad selections, tactical profiles, and player states are
resolved from the existing canonical repositories instead of a separate
simulation copy.

`GET /api/saves/{id}/export` returns a JSON save export containing the schema
version, save metadata, manager profile, career statistics and history, active
tournament state, squad selections, player states, and tactical settings.
`POST /api/saves/import` validates the schema version, manager ownership,
duplicate slot numbers, tournament references, player references, squad
selections, and tactical team references before creating a manual save slot.
Unsupported future save format versions are rejected instead of partially
importing corrupted data.

## Administration core

Phase 9J-1 adds backend-only administrator operations under `/api/admin`.
Every admin endpoint is protected with `ROLE_ADMIN` through Spring Security
method authorization, while the existing JWT authentication flow remains
unchanged.

The admin dashboard endpoint, `GET /api/admin/dashboard`, returns a single
summary payload covering application version and server time, manager counts,
tournament counts, career totals, achievement unlock totals, save-slot totals,
autosave counts, active saves, Redis availability, and database connectivity.

`GET /api/admin/health` returns a compact operational view with Redis status,
database status, active save count, and active tournament count. Infrastructure
checks reuse Actuator health information when available and fall back to direct
database connectivity checks for the datasource.

Tournament administration reuses the existing tournament service instead of
duplicating tournament business logic. Admins can list tournaments, archive
completed tournaments, reopen archived tournaments, reset tournament simulation
state, and delete inactive tournaments:

- `GET /api/admin/tournaments`
- `PUT /api/admin/tournaments/{id}/archive`
- `PUT /api/admin/tournaments/{id}/reopen`
- `POST /api/admin/tournaments/{id}/reset`
- `DELETE /api/admin/tournaments/{id}`

Tournament reset returns the tournament to `UPCOMING`, clears match results,
resets match status, removes generated match events/statistics/player ratings,
and resets standings. Active tournaments cannot be deleted, completed
tournaments must be archived before deletion, and archived tournaments must be
reopened before reset.

Admin tournament actions are persisted in `admin_audit_logs` with the admin
username, action, entity type, entity id, and timestamp. The audit log is kept
lightweight so future admin actions can be added without changing the core
tournament flow.

## Football data management

Phase 9J-2 extends `/api/admin` with administrator-only football dataset
management. Player ratings can be updated individually or in a transactional
bulk request. Bulk updates continue after invalid, duplicate, or unknown player
entries and return updated and failed counts with structured validation
messages. Player lifecycle actions support activation, deactivation, retirement,
and restoration.

National teams can be activated or deactivated, and administrators can update
the linked country's FIFA ranking, a team's confederation, and manager. Squad
refresh recalculates a team's overall rating from its active, non-retired
national player pool and returns validation findings for persisted squads.

`GET /api/admin/dataset/health` reports player and team status totals together
with duplicate-player, invalid-rating, and invalid-squad findings. Dataset
validation checks duplicate names within a national team, rating ranges,
position, nationality, age, preferred foot, squad player references,
national-team eligibility, duplicate squad entries, goalkeeper presence, empty
squads, and the 26-player squad limit.

Dataset actions are auditable through `admin_audit_logs`, including individual
player updates and lifecycle changes, bulk updates, team changes, and squad
refreshes. The data-management APIs are:

- `GET /api/admin/dataset/health`
- `PUT /api/admin/players/{id}`
- `POST /api/admin/players/bulk-update`
- `PUT /api/admin/players/{id}/activate`
- `PUT /api/admin/players/{id}/deactivate`
- `PUT /api/admin/players/{id}/retire`
- `PUT /api/admin/players/{id}/restore`
- `PUT /api/admin/teams/{id}`
- `PUT /api/admin/teams/{id}/activate`
- `PUT /api/admin/teams/{id}/deactivate`
- `POST /api/admin/teams/{id}/refresh-squad`

## Operations and maintenance center

Phase 9J-3 completes the administrator module with operational tooling under
`/api/admin`. Cache operations manage the existing analytics, leaderboard, and
tournament in-memory caches without changing the caching architecture. A cache
request may target `ALL`, `ANALYTICS`, `LEADERBOARD`, or `TOURNAMENT`; rebuild
operations clear the selected cache and warm it from existing persisted data.

Save maintenance lists orphaned save-slot references and can clean orphaned
inactive slots, inactive autosaves, expired backup metadata, and duplicate
backup metadata. Active save slots are never deleted or modified by cleanup.
Maintenance responses report each cleanup count and the affected orphan ids.

Database diagnostics provide public-schema table totals, approximate row totals,
Flyway version and pending migration count, supported PostgreSQL database size,
connection-pool statistics, and core repository counts. System operations
report application uptime, JVM memory, processor count, Java and Spring Boot
versions, Redis/database health, and active simulation/save totals.

Audit history is searchable by username, action, entity type/id, and date range.
Maintenance jobs are persisted separately in `maintenance_history`, retaining
the operation, administrator, duration, status, details, and timestamp; audit
records remain immutable and continue to record each operational action.

The operations endpoints are:

- `POST /api/admin/cache/clear`
- `POST /api/admin/cache/rebuild`
- `POST /api/admin/saves/cleanup`
- `GET /api/admin/database`
- `GET /api/admin/system`
- `GET /api/admin/audit`
- `GET /api/admin/audit/history`
- `GET /api/admin/maintenance`
