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
