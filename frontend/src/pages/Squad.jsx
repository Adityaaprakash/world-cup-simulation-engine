import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { addSquadPlayer, removeSquadPlayer, getMySquads, getSquadPlayers, getSquadAnalysis, trainSquad } from '../api/squadApi'
import { getTeam, getTeamPlayers } from '../api/teamApi'
import { comparePlayers, getPlayerDetails } from '../api/playerApi'
import { retirePlayer, reactivatePlayer } from '../api/contractApi'
import { getTeamTactics, updateTeamTactics } from '../api/tacticsApi'
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
  const [tactics, setTactics] = useState(null)
  const [isSavingTactics, setIsSavingTactics] = useState(false)

  // Filters state
  const [filters, setFilters] = useState({ name: '', position: '', minRating: 0, maxAge: 100 })
  
  // Compare state
  const [compareIds, setCompareIds] = useState([])
  const [comparisonResults, setComparisonResults] = useState(null)
  const [inspectPlayer, setInspectPlayer] = useState(null)

  // Training state
  const [trainingCategory, setTrainingCategory] = useState("TECHNICAL")
  const [trainingIntensity, setTrainingIntensity] = useState("NORMAL")
  const [isTraining, setIsTraining] = useState(false)

  const load = useCallback(async () => {
    setIsLoading(true); setError('')
    try {
      const [teamResponse, playersResponse, squadsResponse, tacticsResponse] = await Promise.all([
        getTeam(teamId), getTeamPlayers(teamId), getMySquads(), getTeamTactics(teamId).catch(() => ({ data: null }))
      ])
      setTeam(teamResponse.data)
      setTactics(tacticsResponse.data)
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

  const handleTrainSquad = async () => {
    if (!squad) return;
    setActionError('');
    setIsTraining(true);
    try {
      await trainSquad(squad.id, trainingCategory, trainingIntensity);
      // Refresh players and analysis to show updated fatigue/stats
      const [playersResponse, analysisResponse] = await Promise.all([
        getTeamPlayers(teamId),
        getSquadAnalysis(squad.id)
      ]);
      setPlayers(playersResponse.data);
      setAnalysis(analysisResponse.data);
    } catch (requestError) {
      setActionError(requestError.response?.data?.message || 'Failed to train squad.');
    } finally {
      setIsTraining(false);
    }
  }

  const handleUpdateTactics = async () => {
    setActionError('');
    setIsSavingTactics(true);
    try {
      const res = await updateTeamTactics(teamId, tactics);
      setTactics(res.data);
    } catch (e) {
      setActionError(e.response?.data?.message || 'Failed to update tactical profile');
    } finally {
      setIsSavingTactics(false);
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
      
      {squad && (
        <Card className="border-emerald-500/30">
          <h2 className="text-lg font-semibold mb-3 text-emerald-400">Squad Training Plan & Development</h2>
          <div className="flex flex-col sm:flex-row items-end flex-wrap gap-4">
            <div className="flex-1 w-full min-w-[200px]">
              <label className="block text-sm font-medium text-slate-300 mb-1">Focus Category</label>
              <select 
                className="w-full px-4 py-2 border border-slate-700 bg-slate-900 rounded-md focus:ring-emerald-500 text-white font-medium"
                value={trainingCategory}
                onChange={(e) => setTrainingCategory(e.target.value)}
              >
                <option value="TECHNICAL">Technical & Skill</option>
                <option value="PHYSICAL">Physical & Fitness</option>
                <option value="TACTICAL">Tactical & Setup</option>
                <option value="MENTAL">Mental & Focus</option>
                <option value="REST">Active Rest</option>
              </select>
            </div>
            <div className="flex-1 w-full min-w-[200px]">
              <label className="block text-sm font-medium text-slate-300 mb-1">Session Intensity</label>
              <select 
                className="w-full px-4 py-2 border border-slate-700 bg-slate-900 rounded-md focus:ring-emerald-500 text-white font-medium"
                value={trainingIntensity}
                onChange={(e) => setTrainingIntensity(e.target.value)}
              >
                <option value="LIGHT">Light Recovery (Low Fatigue)</option>
                <option value="NORMAL">Medium Group (High Growth)</option>
                <option value="INTENSE">Heavy Drill (Max Fatigue)</option>
              </select>
            </div>
            <div className="w-full sm:w-auto">
              <button 
                onClick={handleTrainSquad}
                disabled={isTraining}
                className="w-full inline-flex rounded-lg bg-emerald-500 px-6 py-2 font-semibold text-slate-950 transition hover:bg-emerald-400 disabled:opacity-50"
              >
                {isTraining ? 'Executing Session...' : 'Execute Squad Session'}
              </button>
            </div>
          </div>
          <p className="text-xs text-slate-400 mt-2">
            * Higher intensity yields more progression but heavily spikes fatigue. Ensure players are recovered before a major match.
          </p>
        </Card>
      )}
      
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

      {/* TACTICAL PROFILE SECTION */}
      {squad && tactics && (
        <Card className="border-emerald-500/30">
          <h2 className="text-lg font-semibold mb-3 text-emerald-400">Overarching Tactical Profile</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm mb-4">
             <div>
               <label className="block text-slate-300 font-medium mb-1">Attacking Approach</label>
               <select className="w-full bg-slate-900 border border-slate-700 rounded px-2 py-1 text-white" value={tactics.attackingApproach} onChange={e => setTactics({...tactics, attackingApproach: e.target.value})}>
                 <option value="ATTACKING">Attacking</option>
                 <option value="BALANCED">Balanced</option>
                 <option value="CONSERVATIVE">Conservative</option>
               </select>
             </div>
             <div>
               <label className="block text-slate-300 font-medium mb-1">Build Up Style</label>
               <select className="w-full bg-slate-900 border border-slate-700 rounded px-2 py-1 text-white" value={tactics.buildUpStyle} onChange={e => setTactics({...tactics, buildUpStyle: e.target.value})}>
                 <option value="POSSESSION">Possession</option>
                 <option value="BALANCED">Balanced</option>
                 <option value="DIRECT">Direct</option>
               </select>
             </div>
             <div>
               <label className="block text-slate-300 font-medium mb-1">Passing Style</label>
               <select className="w-full bg-slate-900 border border-slate-700 rounded px-2 py-1 text-white" value={tactics.passingStyle} onChange={e => setTactics({...tactics, passingStyle: e.target.value})}>
                 <option value="SHORT">Short</option>
                 <option value="MIXED">Mixed</option>
                 <option value="DIRECT">Direct</option>
               </select>
             </div>
             <div>
               <label className="block text-slate-300 font-medium mb-1">Tempo</label>
               <select className="w-full bg-slate-900 border border-slate-700 rounded px-2 py-1 text-white" value={tactics.tempo} onChange={e => setTactics({...tactics, tempo: e.target.value})}>
                 <option value="FAST">Fast</option>
                 <option value="BALANCED">Balanced</option>
                 <option value="SLOW">Slow</option>
               </select>
             </div>
             <div>
               <label className="block text-slate-300 font-medium mb-1">Attacking Width</label>
               <select className="w-full bg-slate-900 border border-slate-700 rounded px-2 py-1 text-white" value={tactics.width} onChange={e => setTactics({...tactics, width: e.target.value})}>
                 <option value="WIDE">Wide</option>
                 <option value="BALANCED">Balanced</option>
                 <option value="NARROW">Narrow</option>
               </select>
             </div>
             <div>
               <label className="block text-slate-300 font-medium mb-1">Pressing Intensity</label>
               <select className="w-full bg-slate-900 border border-slate-700 rounded px-2 py-1 text-white" value={tactics.pressingIntensity} onChange={e => setTactics({...tactics, pressingIntensity: e.target.value})}>
                 <option value="HIGH">High</option>
                 <option value="BALANCED">Balanced</option>
                 <option value="LOW">Low</option>
               </select>
             </div>
             <div>
               <label className="block text-slate-300 font-medium mb-1">Defensive Line</label>
               <select className="w-full bg-slate-900 border border-slate-700 rounded px-2 py-1 text-white" value={tactics.defensiveLine} onChange={e => setTactics({...tactics, defensiveLine: e.target.value})}>
                 <option value="HIGH">High</option>
                 <option value="BALANCED">Balanced</option>
                 <option value="DEEP">Deep</option>
               </select>
             </div>
             <div>
               <label className="block text-slate-300 font-medium mb-1">Defensive Block</label>
               <select className="w-full bg-slate-900 border border-slate-700 rounded px-2 py-1 text-white" value={tactics.defensiveBlock} onChange={e => setTactics({...tactics, defensiveBlock: e.target.value})}>
                 <option value="HIGH_BLOCK">High Block</option>
                 <option value="MID_BLOCK">Mid Block</option>
                 <option value="LOW_BLOCK">Low Block</option>
               </select>
             </div>
          </div>
          <div className="flex justify-end mt-4">
             <Button variant="primary" onClick={handleUpdateTactics} disabled={isSavingTactics}>
               {isSavingTactics ? 'Saving...' : 'Save Tactical Profile'}
             </Button>
          </div>
        </Card>
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
                 <p>Fit: {p.fitness}% | Fat: {p.fatigue}%</p>
                 <p>Form: {p.currentForm}% | WkLd: {p.workload}%</p>
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
             <div><p className="text-emerald-300 font-semibold text-sm">Fatigue</p><p className="text-white font-bold">{inspectPlayer.fatigue}%</p></div>
             <div>
                <p className="text-emerald-300 font-semibold text-sm">Workload</p>
                <p className={`font-bold ${inspectPlayer.workload > 75 ? 'text-red-500' : 'text-white'}`}>
                   {inspectPlayer.workload}% {inspectPlayer.workload > 75 && '(High Risk)'}
                </p>
             </div>
             {inspectPlayer.injuryStatus !== 'HEALTHY' && (
               <div><p className="text-red-400 font-semibold text-sm">Injury</p><p className="text-red-500 font-bold">{inspectPlayer.injuryStatus}</p></div>
             )}
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
