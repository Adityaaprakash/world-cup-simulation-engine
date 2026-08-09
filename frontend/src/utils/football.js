const POSITION_GROUPS = {
  Goalkeepers: ['GK'],
  Defenders: ['RB', 'CB', 'LB'],
  Midfielders: ['CDM', 'CM', 'CAM'],
  Forwards: ['RW', 'LW', 'ST'],
}

export const positionGroups = (players) => Object.entries(POSITION_GROUPS).map(([label, positions]) => ({
  label,
  players: players.filter((player) => positions.includes(player.position)),
}))

export const formatLabel = (value) => value
  ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase())
  : '—'

export const LINEUP_POSITION_SLOTS = ['GK', 'LB', 'CB', 'RB', 'LWB', 'RWB', 'CDM', 'CM', 'CAM', 'LM', 'RM', 'LW', 'RW', 'ST', 'CF']
