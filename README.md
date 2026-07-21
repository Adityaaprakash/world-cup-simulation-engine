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
