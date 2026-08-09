import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { assignPosition, getLineup, getMySquads, getSquadPlayers, getSquadReadyStatus, setCaptain, updateStartingXi, validateLineup } from '../api/squadApi'
import { getTeam } from '../api/teamApi'
import Button from '../components/common/Button'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import FormationPitch from '../components/squad/FormationPitch'
import PlayerCard from '../components/squad/PlayerCard'
import { LINEUP_POSITION_SLOTS } from '../utils/football'

export default function LineupBuilder() {
  const { teamId } = useParams()
  const [team, setTeam] = useState(null)
  const [squad, setSquad] = useState(null)
  const [players, setPlayers] = useState([])
  const [starters, setStarters] = useState([])
  const [positions, setPositions] = useState({})
  const [captainId, setCaptainId] = useState('')
  const [activePlayerId, setActivePlayerId] = useState(null)
  const [validation, setValidation] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setIsLoading(true); setError('')
    try {
      const [teamResponse, squadsResponse] = await Promise.all([getTeam(teamId), getMySquads()])
      setTeam(teamResponse.data)
      const matchingSquad = squadsResponse.data.find((item) => item.teamName === teamResponse.data.name) || null
      setSquad(matchingSquad)
      if (!matchingSquad) return
      const [playersResponse, lineupResponse, validationResponse] = await Promise.all([getSquadPlayers(matchingSquad.id), getLineup(matchingSquad.id), validateLineup(matchingSquad.id)])
      const lineupByPlayerId = new Map(lineupResponse.data.map((entry) => [entry.playerId, entry]))
      const selected = playersResponse.data.filter((player) => lineupByPlayerId.get(player.id)?.startingXi)
      setPlayers(playersResponse.data)
      setStarters(selected.map((player) => player.id))
      setPositions(Object.fromEntries(selected.map((player) => [player.id, lineupByPlayerId.get(player.id)?.positionSlot === 'RESERVE' ? player.position : lineupByPlayerId.get(player.id)?.positionSlot || player.position])))
      setCaptainId(lineupResponse.data.find((entry) => entry.captain)?.playerId?.toString() || '')
      setValidation(validationResponse.data)
    } catch (requestError) { setError(requestError.message || 'Unable to load the lineup builder.') } finally { setIsLoading(false) }
  }, [teamId])

  useEffect(() => { load() }, [load])

  const selectedPlayers = useMemo(() => players.filter((player) => starters.includes(player.id)).map((player) => ({ ...player, positionSlot: positions[player.id] || player.position })), [players, starters, positions])
  const toggleStarter = (playerId) => {
    setError('')
    if (starters.includes(playerId)) {
      setStarters((current) => current.filter((id) => id !== playerId))
      setPositions((current) => { const next = { ...current }; delete next[playerId]; return next })
      if (captainId === String(playerId)) setCaptainId('')
      return
    }
    if (starters.length === 11) { setError('A starting XI can contain exactly 11 players. Remove a player before adding another.'); return }
    const player = players.find((item) => item.id === playerId)
    setStarters((current) => [...current, playerId])
    setPositions((current) => ({ ...current, [playerId]: player.position }))
    setActivePlayerId(playerId)
  }

  const saveLineup = async () => {
    if (starters.length !== 11) { setError('Select exactly 11 players before saving the starting XI.'); return }
    if (selectedPlayers.filter((player) => player.positionSlot === 'GK').length !== 1) { setError('Assign exactly one goalkeeper position before saving.'); return }
    if (!captainId) { setError('Select a captain from the starting XI before saving.'); return }
    setIsSaving(true); setError('')
    try {
      await updateStartingXi(squad.id, starters)
      await Promise.all(selectedPlayers.map((player) => assignPosition(squad.id, player.id, player.positionSlot)))
      await setCaptain(squad.id, Number(captainId))
      const [validationResponse, readyResponse] = await Promise.all([validateLineup(squad.id), getSquadReadyStatus(squad.id)])
      setValidation({ ...validationResponse.data, readyMessage: readyResponse.data.message, ready: readyResponse.data.ready })
      if (!validationResponse.data.valid || !readyResponse.data.ready) setError(readyResponse.data.message || validationResponse.data.message)
    } catch (requestError) { setError(requestError.message || 'Unable to save the lineup.') } finally { setIsSaving(false) }
  }

  if (isLoading) return <Loading label="Loading your lineup..." />
  if (!team) return <ErrorMessage message={error || 'Team not found.'} />
  if (!squad) return <div className="space-y-5"><Link to={`/teams/${teamId}/squad`} className="text-sm font-semibold text-emerald-400">← Back to squad</Link><EmptyState title="No user squad available" description="An existing user-owned squad is required to build a lineup. The backend does not expose formations for creating one from the frontend." /></div>

  return <div className="space-y-7"><Link to={`/teams/${teamId}/squad`} className="text-sm font-semibold text-emerald-400 hover:text-emerald-300">← Back to {team.name} squad</Link><div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Starting XI</p><h1 className="mt-2 text-3xl font-bold text-white">{team.name} lineup</h1><p className="mt-2 text-slate-400">{squad.name} · Current formation: {squad.formationName}</p></div><Button onClick={saveLineup} disabled={isSaving}>{isSaving ? 'Saving lineup...' : 'Save starting XI'}</Button></div><ErrorMessage message={error} />{validation && <p className={`rounded-lg border px-3 py-2 text-sm ${validation.valid && validation.ready !== false ? 'border-emerald-500/40 bg-emerald-500/10 text-emerald-200' : 'border-amber-500/40 bg-amber-500/10 text-amber-100'}`}>{validation.readyMessage || validation.message}</p>}<div className="grid gap-6 xl:grid-cols-[minmax(0,1.15fr)_minmax(22rem,0.85fr)]"><Card><div className="mb-4 flex items-center justify-between"><div><h2 className="text-lg font-bold">Formation pitch</h2><p className="mt-1 text-sm text-slate-400">Click a selected player to edit their position assignment.</p></div><span className="rounded-full bg-slate-800 px-3 py-1 text-sm font-semibold text-slate-200">{starters.length}/11 selected</span></div><FormationPitch players={selectedPlayers} activePlayerId={activePlayerId} onSelectPlayer={setActivePlayerId} /></Card><Card><h2 className="text-lg font-bold">Position assignments</h2><p className="mt-1 text-sm text-slate-400">Slots use the backend’s accepted position values. Formation compatibility is confirmed by backend validation.</p>{selectedPlayers.length ? <div className="mt-4 space-y-3">{selectedPlayers.map((player) => <div key={player.id} className={`rounded-lg border p-3 ${activePlayerId === player.id ? 'border-amber-300 bg-amber-300/10' : 'border-slate-800 bg-slate-950/40'}`}><div className="flex items-center justify-between gap-3"><button type="button" className="text-left" onClick={() => setActivePlayerId(player.id)}><p className="font-semibold text-slate-100">{player.name}</p><p className="text-xs text-slate-500">Natural position: {player.position}</p></button><select value={positions[player.id] || player.position} onChange={(event) => setPositions((current) => ({ ...current, [player.id]: event.target.value }))} className="rounded-lg border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-white outline-none focus:border-emerald-400">{LINEUP_POSITION_SLOTS.map((slot) => <option key={slot}>{slot}</option>)}</select></div></div>)}</div> : <EmptyState title="Choose your XI" description="Select players from the squad list to place them on the pitch." />}<label className="mt-5 block text-sm font-semibold text-slate-200">Captain<select value={captainId} onChange={(event) => setCaptainId(event.target.value)} className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-white outline-none focus:border-emerald-400"><option value="">Select a starting player</option>{selectedPlayers.map((player) => <option key={player.id} value={player.id}>{player.name}</option>)}</select></label></Card></div><section><div className="mb-4"><h2 className="text-xl font-bold text-white">Squad selection</h2><p className="mt-1 text-sm text-slate-400">Choose exactly 11 starters from players already added to your backend squad. Remaining players stay as non-starting reserve entries in the backend.</p></div>{players.length ? <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{players.map((player) => <PlayerCard key={player.id} player={player} selected={starters.includes(player.id)} inSquad actionLabel={starters.includes(player.id) ? 'Remove from XI' : 'Add to XI'} onAction={() => toggleStarter(player.id)} />)}</div> : <EmptyState title="Your squad is empty" description="Add team roster players from the squad management page before building a lineup." />}</section></div>
}
