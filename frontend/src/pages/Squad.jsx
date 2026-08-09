import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { addSquadPlayer, getMySquads, getSquadPlayers } from '../api/squadApi'
import { getTeam, getTeamPlayers } from '../api/teamApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import PlayerCard from '../components/squad/PlayerCard'
import { positionGroups } from '../utils/football'

export default function Squad() {
  const { teamId } = useParams()
  const [team, setTeam] = useState(null)
  const [players, setPlayers] = useState([])
  const [squad, setSquad] = useState(null)
  const [squadPlayerIds, setSquadPlayerIds] = useState(new Set())
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [pendingPlayerId, setPendingPlayerId] = useState(null)

  const load = useCallback(async () => {
    setIsLoading(true); setError('')
    try {
      const [teamResponse, playersResponse, squadsResponse] = await Promise.all([getTeam(teamId), getTeamPlayers(teamId), getMySquads()])
      setTeam(teamResponse.data); setPlayers(playersResponse.data)
      const matchingSquad = squadsResponse.data.find((item) => item.teamName === teamResponse.data.name) || null
      setSquad(matchingSquad)
      if (matchingSquad) {
        const { data } = await getSquadPlayers(matchingSquad.id)
        setSquadPlayerIds(new Set(data.map((player) => player.id)))
      } else setSquadPlayerIds(new Set())
    } catch (requestError) { setError(requestError.message || 'Unable to load this squad.') } finally { setIsLoading(false) }
  }, [teamId])

  useEffect(() => { load() }, [load])
  const groups = useMemo(() => positionGroups(players), [players])
  const addPlayer = async (playerId) => { if (!squad) return; setActionError(''); setPendingPlayerId(playerId); try { await addSquadPlayer(squad.id, playerId); setSquadPlayerIds((current) => new Set([...current, playerId])) } catch (requestError) { setActionError(requestError.message || 'Unable to add this player to your squad.') } finally { setPendingPlayerId(null) } }

  if (isLoading) return <Loading label="Loading team squad..." />
  if (!team) return <ErrorMessage message={error || 'Team not found.'} />
  return <div className="space-y-7"><Link to="/teams" className="text-sm font-semibold text-emerald-400 hover:text-emerald-300">← All national teams</Link><Card><div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Squad Management</p><h1 className="mt-2 text-3xl font-bold text-white">{team.name}</h1><p className="mt-2 text-slate-400">{players.length} roster players · {team.overallRating} overall team rating</p></div>{squad && <Link to={`/teams/${teamId}/lineup`} className="inline-flex rounded-lg bg-emerald-500 px-4 py-2 font-semibold text-slate-950 transition hover:bg-emerald-400">Open lineup builder</Link>}</div></Card><ErrorMessage message={error || actionError} />{squad ? <Card className="border-emerald-500/30"><p className="font-semibold text-emerald-300">Your squad: {squad.name}</p><p className="mt-1 text-sm text-slate-400">{squadPlayerIds.size}/26 players selected · Formation: {squad.formationName}</p></Card> : <EmptyState title="No user squad for this team" description="The backend requires a formation id to create a squad, but it does not expose the formation catalog to the frontend. An existing squad can be managed here." />}{squad && <p className="text-sm text-slate-400">Add eligible roster players to your squad before selecting a starting XI. The backend limits a squad to 26 players.</p>}{groups.map(({ label, players: groupPlayers }) => groupPlayers.length ? <section key={label}><h2 className="mb-3 text-lg font-bold text-slate-100">{label} <span className="text-sm font-normal text-slate-500">({groupPlayers.length})</span></h2><div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{groupPlayers.map((player) => <PlayerCard key={player.id} player={player} inSquad={squadPlayerIds.has(player.id)} actionLabel={squadPlayerIds.has(player.id) ? undefined : 'Add to squad'} onAction={() => addPlayer(player.id)} disabled={!squad || squadPlayerIds.size >= 26 || pendingPlayerId === player.id} />)}</div></section> : null)}</div>
}
