import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { addSquadPlayer, removeSquadPlayer, getMySquads, getSquadPlayers, getSquadAnalysis } from '../api/squadApi'
import { getTeam, getTeamPlayers } from '../api/teamApi'
import { comparePlayers, getPlayerDetails } from '../api/playerApi'
import { retirePlayer, reactivatePlayer } from '../api/contractApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import PlayerCard from '../components/squad/PlayerCard'
import Button from '../components/common/Button'

export default function Squad() {
  const { teamId } = useParams()
  const [team, setTeam] = useState(null)
  const [players, setPlayers] = useState([])
  const [squad, setSquad] = useState(null)
  const [squadPlayerIds, setSquadPlayerIds] = useState(new Set())
  const [analysis, setAnalysis] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [pendingPlayerId, setPendingPlayerId] = useState(null)

  // Filters state
  const [filters, setFilters] = useState({ name: '', position: '', minRating: 0, maxAge: 100 })
  
  // Compare state
  const [compareIds, setCompareIds] = useState([])
  const [comparisonResults, setComparisonResults] = useState(null)
  const [inspectPlayer, setInspectPlayer] = useState(null)

  const load = useCallback(async () => {
    setIsLoading(true); setError('')
    try {
      const [teamResponse, playersResponse, squadsResponse] = await Promise.all([
        getTeam(teamId), getTeamPlayers(teamId), getMySquads()
      ])
      setTeam(teamResponse.data)
      setPlayers(playersResponse.data)
      
      const matchingSquad = squadsResponse.data.find((item) => item.teamName === teamResponse.data.name) || null
      setSquad(matchingSquad)
      if (matchingSquad) {
        const { data } = await getSquadPlayers(matchingSquad.id)
        setSquadPlayerIds(new Set(data.map((player) => player.id)))
        const r = await getSquadAnalysis(matchingSquad.id)
        setAnalysis(r.data)
      } else {
        setSquadPlayerIds(new Set())
      }
    } catch (requestError) { 
      setError(requestError.message || 'Unable to load this squad.') 
    } finally { 
      setIsLoading(false) 
    }
  }, [teamId])

  useEffect(() => { load() }, [load])

  const filteredPlayers = useMemo(() => {
    return players.filter(p => {
      if (filters.name && !p.name.toLowerCase().includes(filters.name.toLowerCase())) return false
      if (filters.position && p.position !== filters.position) return false
      if (filters.minRating > 0 && p.overallRating < filters.minRating) return false
      if (filters.maxAge < 100 && p.age > filters.maxAge) return false
      return true
    })
  }, [players, filters])

  const addPlayer = async (playerId) => { 
    if (!squad) return; 
    setActionError(''); 
    setPendingPlayerId(playerId); 
    try { 
      await addSquadPlayer(squad.id, playerId); 
      setSquadPlayerIds(current => new Set([...current, playerId]))
      const r = await getSquadAnalysis(squad.id)
      setAnalysis(r.data)
    } catch (requestError) { 
      setActionError(requestError.response?.data?.message || 'Unable to add this player to your squad.') 
    } finally { 
      setPendingPlayerId(null) 
    } 
  }

  const removePlayer = async (playerId) => {
    if (!squad) return;
    setActionError('');
    setPendingPlayerId(playerId);
    try {
      await removeSquadPlayer(squad.id, playerId);
      setSquadPlayerIds(current => {
        const next = new Set(current)
        next.delete(playerId)
        return next
      })
      const r = await getSquadAnalysis(squad.id)
      setAnalysis(r.data)
    } catch (requestError) {
      setActionError(requestError.response?.data?.message || 'Unable to remove player.')
    } finally {
      setPendingPlayerId(null)
    }
  }

  const handleCompareToggle = (id) => {
    setCompareIds(curr => {
      if (curr.includes(id)) return curr.filter(x => x !== id);
      if (curr.length < 5) return [...curr, id];
      return curr;
    })
  }

  const runComparison = async () => {
    if (compareIds.length < 2) return;
    setActionError('');
    try {
      const res = await comparePlayers(compareIds)
      setComparisonResults(res.data)
    } catch (e) {
      setActionError('Failed to compare players')
    }
  }

  const showDetails = async (id) => {
    try {
      const res = await getPlayerDetails(id)
      setInspectPlayer(res.data)
    } catch (e) {}
  }

  const handleToggleRetirement = async (id, isRetired) => {
    try {
      if (isRetired) {
        await reactivatePlayer(id)
      } else {
        await retirePlayer(id)
      }
      showDetails(id)
    } catch (e) {
      setActionError(e.response?.data?.message || 'Failed to update retirement status')
    }
  }

  if (isLoading) return <Loading label="Loading team squad..." />
  if (!team) return <ErrorMessage message={error || 'Team not found.'} />

  return (
    <div className="space-y-7">
      <Link to="/teams" className="text-sm font-semibold text-emerald-400 hover:text-emerald-300">← All national teams</Link>
      <Card>
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Squad Management</p>
            <h1 className="mt-2 text-3xl font-bold text-white">{team.name}</h1>
            <p className="mt-2 text-slate-400">{players.length} valid national players eligible for call-up</p>
          </div>
          {squad && <Link to={`/teams/${teamId}/lineup`} className="inline-flex rounded-lg bg-emerald-500 px-4 py-2 font-semibold text-slate-950 transition hover:bg-emerald-400">Open lineup builder</Link>}
        </div>
      </Card>
      <ErrorMessage message={error || actionError} />
      
      {squad ? (
        <Card className="border-emerald-500/30">
          <p className="font-semibold text-emerald-300">Active Squad: {squad.name}</p>
          {analysis && (
            <div className="mt-4 grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div className="bg-slate-900 rounded p-3 text-center border border-slate-700">GK: {analysis.goalkeeperCount}</div>
              <div className="bg-slate-900 rounded p-3 text-center border border-slate-700">DEF: {analysis.defenderCount}</div>
              <div className="bg-slate-900 rounded p-3 text-center border border-slate-700">MID: {analysis.midfielderCount}</div>
              <div className="bg-slate-900 rounded p-3 text-center border border-slate-700">ATT: {analysis.attackerCount}</div>
              <div className="col-span-2 md:col-span-4 bg-slate-800 p-3 rounded">
                <span className="font-bold text-white">Size:</span> {analysis.totalPlayers}/26 <br/>
                <span className="font-bold text-emerald-300">Analyst Recommendation:</span> {analysis.recommendation}
              </div>
            </div>
          )}
        </Card>
      ) : (
        <EmptyState title="No active squad configuration" description="An existing career save is required to manage this squad." />
      )}

      {/* Comparisons & Inspectors */}
      {comparisonResults && (
         <Card className="border-emerald-500 bg-emerald-900/10">
           <div className="flex justify-between items-center mb-4">
             <h3 className="font-bold text-white">Scouting Comparison</h3>
             <Button variant="secondary" onClick={() => setComparisonResults(null)}>Close</Button>
           </div>
           <div className="flex gap-4 overflow-x-auto pb-2">
             {comparisonResults.map(p => (
               <div key={p.id} className="bg-slate-900 p-4 rounded min-w-[200px] text-sm">
                 <p className="font-bold text-emerald-400 mb-2">{p.name}</p>
                 <p>OVR: <span className="text-white font-bold">{p.overallRating}</span></p>
                 <p>PAC/SHO/PAS: {p.pace}/{p.shooting}/{p.passing}</p>
                 <p>DRI/DEF/PHY: {p.dribbling}/{p.defending}/{p.physical}</p>
                 <p>Form: {p.currentForm}%</p>
               </div>
             ))}
           </div>
         </Card>
      )}

      {inspectPlayer && (
        <Card className="border-emerald-500 border-2 relative">
           {inspectPlayer.retired && (
             <div className="absolute -top-3 left-4 bg-red-600 font-bold tracking-widest text-white px-2 py-1 rounded text-xs">RETIRED</div>
           )}
           <div className="flex justify-between items-center">
             <h3 className="text-xl font-bold text-white">{inspectPlayer.name} Profile</h3>
             <div className="flex gap-2 items-center">
               <Button variant="secondary" onClick={() => handleToggleRetirement(inspectPlayer.id, inspectPlayer.retired)}>
                 {inspectPlayer.retired ? 'Reactivate' : 'Retire Player'}
               </Button>
               <Button variant="primary" onClick={() => setInspectPlayer(null)}>Close</Button>
             </div>
           </div>
           <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
             <div><p className="text-emerald-300 font-semibold text-sm">Age</p><p className="text-white font-bold">{inspectPlayer.age}</p></div>
             <div><p className="text-emerald-300 font-semibold text-sm">POT</p><p className="text-white font-bold">{inspectPlayer.potential}</p></div>
             <div><p className="text-emerald-300 font-semibold text-sm">Form</p><p className="text-white font-bold">{inspectPlayer.currentForm}%</p></div>
             <div><p className="text-emerald-300 font-semibold text-sm">Fitness</p><p className="text-white font-bold">{inspectPlayer.fitness}%</p></div>
           </div>
        </Card>
      )}

      <Card>
        <div className="mb-4 flex flex-wrap gap-4 items-center justify-between">
          <div className="flex gap-4 items-center">
            <input type="text" placeholder="Search name..." className="bg-slate-900 border border-slate-700 rounded px-3 py-1" value={filters.name} onChange={e => setFilters({...filters, name: e.target.value})} />
            <select className="bg-slate-900 border border-slate-700 rounded px-3 py-1" value={filters.position} onChange={e => setFilters({...filters, position: e.target.value})}>
              <option value="">Any Pos</option>
              {['GK', 'CB', 'LB', 'RB', 'CDM', 'CM', 'CAM', 'LM', 'RM', 'LW', 'RW', 'ST'].map(p => <option key={p} value={p}>{p}</option>)}
            </select>
          </div>
          <div>
            <Button variant="secondary" onClick={runComparison} disabled={compareIds.length < 2}>Compare Selected ({compareIds.length}/5)</Button>
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {filteredPlayers.map((player) => {
            const inSquad = squadPlayerIds.has(player.id)
            const isComparing = compareIds.includes(player.id)
            return (
              <div key={player.id} className="relative group">
                <PlayerCard 
                  player={player} 
                  inSquad={inSquad}
                  actionLabel={inSquad ? 'Drop Player' : 'Call Up'}
                  disabled={!squad || pendingPlayerId === player.id || (!inSquad && squadPlayerIds.size >= 26)}
                  onAction={() => inSquad ? removePlayer(player.id) : addPlayer(player.id)}
                />
                <div className="absolute top-2 right-2 flex gap-2">
                  <button onClick={() => showDetails(player.id)} className="bg-blue-600/80 hover:bg-blue-500 rounded px-2 py-1 text-xs text-white">Inspect</button>
                  <button onClick={() => handleCompareToggle(player.id)} className={`${isComparing ? 'bg-purple-600' : 'bg-slate-700'} hover:bg-purple-500 rounded px-2 py-1 text-xs text-white`}>Comp</button>
                </div>
              </div>
            )
          })}
        </div>
      </Card>
    </div>
  )
}
