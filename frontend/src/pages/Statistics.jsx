import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getFootballRecords, getPagedTournamentStatistics, getPlayerStatistics, getStatisticsSummary, getTeamStatistics, getTopAssists, getTopCleanSheets, getTopRatings, getTopScorers } from '../api/statisticsApi'
import Card from '../components/common/Card'
import EmptyState from '../components/common/EmptyState'
import ErrorMessage from '../components/common/ErrorMessage'
import Loading from '../components/common/Loading'
import Pagination from '../components/common/Pagination'
import Leaderboard from '../components/statistics/Leaderboard'

const tabs = ['Overview', 'Players', 'Teams', 'Tournaments', 'Records']
const display = (value, digits) => value == null ? '—' : typeof value === 'number' && digits != null ? value.toFixed(digits) : value

function Metric({ label, value }) { return <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className="mt-1 text-2xl font-bold text-slate-100">{display(value, typeof value === 'number' && !Number.isInteger(value) ? 2 : undefined)}</p></div> }

function Table({ headers, rows }) { return <div className="overflow-x-auto"><table className="min-w-max w-full text-sm"><thead className="border-b border-slate-800 text-left text-xs uppercase tracking-wide text-slate-500"><tr>{headers.map((header) => <th key={header} className="px-3 py-3">{header}</th>)}</tr></thead><tbody>{rows}</tbody></table></div> }

function Records({ records }) {
  const sections = records ? [
    ['Player records', records.playerRecords, [['Top scorers', 'topScorers'], ['Top assists', 'topAssisters'], ['Clean sheets', 'mostCleanSheets'], ['Highest average ratings', 'highestAverageRatings']]],
    ['Team records', records.teamRecords, [['Longest winning streaks', 'longestWinningStreaks'], ['Longest unbeaten streaks', 'longestUnbeatenStreaks'], ['Biggest victories', 'biggestVictories'], ['Most titles', 'mostTitles']]],
    ['Match records', records.matchRecords, [['Highest-scoring matches', 'highestScoringMatches'], ['Biggest comebacks', 'biggestComebacks'], ['Longest penalty shootouts', 'longestPenaltyShootouts'], ['Most cards', 'mostCards']]],
  ] : []
  if (!sections.length) return <EmptyState title="Records unavailable" description="The backend has not returned football records." />
  return <div className="grid gap-6 xl:grid-cols-3">{sections.map(([title, group, lists]) => <Card key={title}><h2 className="text-lg font-bold text-white">{title}</h2>{lists.map(([label, key]) => group?.[key]?.length ? <div key={key} className="mt-5"><h3 className="text-sm font-semibold text-emerald-300">{label}</h3><ol className="mt-2 space-y-2">{group[key].slice(0, 5).map((record, index) => <li key={`${record.id}-${index}`} className="rounded-lg border border-slate-800 bg-slate-950/50 p-3"><p className="font-semibold text-slate-100">{record.name}</p><p className="mt-1 text-xs text-slate-400">{record.detail}</p><p className="mt-1 text-sm font-bold text-emerald-300">{record.value}</p></li>)}</ol></div> : null)}</Card>)}</div>
}

export default function Statistics() {
  const [tab, setTab] = useState('Overview')
  const [summary, setSummary] = useState(null)
  const [leaderboards, setLeaderboards] = useState({})
  const [records, setRecords] = useState(null)
  const [pageData, setPageData] = useState(null)
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [tableLoading, setTableLoading] = useState(false)
  const [error, setError] = useState('')
  const [tableError, setTableError] = useState('')

  useEffect(() => {
    Promise.allSettled([getStatisticsSummary(), getTopScorers(), getTopAssists(), getTopCleanSheets(), getTopRatings(), getFootballRecords()]).then((results) => {
      const [summaryResult, scorers, assists, cleanSheets, ratings, recordsResult] = results
      setSummary(summaryResult.status === 'fulfilled' ? summaryResult.value.data : null)
      setLeaderboards({ scorers: scorers.status === 'fulfilled' ? scorers.value.data : [], assists: assists.status === 'fulfilled' ? assists.value.data : [], cleanSheets: cleanSheets.status === 'fulfilled' ? cleanSheets.value.data : [], ratings: ratings.status === 'fulfilled' ? ratings.value.data : [] })
      setRecords(recordsResult.status === 'fulfilled' ? recordsResult.value.data : null)
      const failed = results.find((result) => result.status === 'rejected')
      if (failed) setError(failed.reason?.message || 'Some statistics sections are unavailable.')
      setIsLoading(false)
    })
  }, [])

  const loadPage = useCallback(async () => {
    if (!['Players', 'Teams', 'Tournaments'].includes(tab)) { setPageData(null); return }
    setTableLoading(true); setTableError('')
    const params = { page, size: 15 }
    if (query.trim()) params.name = query.trim()
    try {
      const request = tab === 'Players' ? getPlayerStatistics(params) : tab === 'Teams' ? getTeamStatistics(params) : getPagedTournamentStatistics(params)
      const { data } = await request
      setPageData(data)
    } catch (requestError) { setTableError(requestError.message || 'Unable to load this statistics table.') } finally { setTableLoading(false) }
  }, [tab, page, query])

  useEffect(() => { loadPage() }, [loadPage])
  const switchTab = (nextTab) => { setTab(nextTab); setPage(0); setQuery('') }
  if (isLoading) return <Loading label="Loading statistics hub..." />

  const playerRows = pageData?.content?.map((player) => <tr key={player.id} className="border-b border-slate-800/80"><td className="px-3 py-3 font-semibold text-slate-100">{player.name}<span className="ml-2 text-xs text-slate-500">{player.position}</span></td><td className="px-3 py-3 text-slate-400">{player.country}</td><td className="px-3 py-3">{player.goals}</td><td className="px-3 py-3">{player.assists}</td><td className="px-3 py-3">{player.cleanSheets}</td><td className="px-3 py-3">{display(player.averageRating, 2)}</td><td className="px-3 py-3">{player.minutesPlayed}</td></tr>)
  const teamRows = pageData?.content?.map((team) => <tr key={team.teamId} className="border-b border-slate-800/80"><td className="px-3 py-3 font-semibold text-slate-100">{team.teamName}</td><td className="px-3 py-3">{team.matchesPlayed}</td><td className="px-3 py-3">{team.wins}</td><td className="px-3 py-3">{team.draws}</td><td className="px-3 py-3">{team.losses}</td><td className="px-3 py-3">{team.goalsScored}</td><td className="px-3 py-3">{team.goalsConceded}</td><td className="px-3 py-3">{team.cleanSheets}</td><td className="px-3 py-3">{display(team.winPercentage, 1)}%</td></tr>)
  const tournamentRows = pageData?.content?.map((tournament) => <tr key={tournament.tournamentId} className="border-b border-slate-800/80"><td className="px-3 py-3 font-semibold text-slate-100"><Link to={`/tournaments/${tournament.tournamentId}/summary`} className="hover:text-emerald-300">{tournament.tournamentName}</Link></td><td className="px-3 py-3">{tournament.year}</td><td className="px-3 py-3">{tournament.status}</td><td className="px-3 py-3">{tournament.completedMatches}</td><td className="px-3 py-3">{tournament.totalGoals}</td><td className="px-3 py-3">{display(tournament.averageGoals, 2)}</td><td className="px-3 py-3">{tournament.cleanSheets}</td><td className="px-3 py-3">{tournament.champion || '—'}</td></tr>)

  return <div className="space-y-7"><div><p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-400">Analytics Centre</p><h1 className="mt-2 text-3xl font-bold text-white">Awards & statistics</h1><p className="mt-2 text-slate-400">Backend-powered rankings, performance data, tournament reports, and football records.</p></div><ErrorMessage message={error} /><nav className="flex flex-wrap gap-2" aria-label="Statistics sections">{tabs.map((item) => <button key={item} type="button" onClick={() => switchTab(item)} className={`rounded-lg px-4 py-2 text-sm font-semibold transition ${tab === item ? 'bg-emerald-500 text-slate-950' : 'bg-slate-800 text-slate-300 hover:bg-slate-700'}`}>{item}</button>)}</nav>{tab === 'Overview' && <><section className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-7">{summary ? <><Metric label="Tournaments" value={summary.totalTournaments} /><Metric label="Teams" value={summary.totalTeams} /><Metric label="Players" value={summary.totalPlayers} /><Metric label="Matches" value={summary.totalMatchesSimulated} /><Metric label="Goals" value={summary.totalGoalsScored} /><Metric label="Avg. goals" value={summary.averageGoalsPerMatch} /><Metric label="Managers" value={summary.activeManagers} /></> : <EmptyState title="Summary unavailable" description="The backend did not return global summary metrics." />}</section><div className="grid gap-6 xl:grid-cols-2"><Leaderboard title="Top scorers" entries={leaderboards.scorers} valueLabel="goals" /><Leaderboard title="Top assists" entries={leaderboards.assists} valueLabel="assists" /><Leaderboard title="Clean-sheet leaders" entries={leaderboards.cleanSheets} valueLabel="clean sheets" /><Leaderboard title="Highest-rated players" entries={leaderboards.ratings} valueLabel="rating" rating /></div></>}{['Players', 'Teams', 'Tournaments'].includes(tab) && <Card><div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div><h2 className="text-xl font-bold text-white">{tab} statistics</h2><p className="mt-1 text-sm text-slate-400">Server-side paging and name filtering.</p></div><label className="text-sm font-medium text-slate-300">Search by name<input value={query} onChange={(event) => { setQuery(event.target.value); setPage(0) }} className="mt-1 block rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-white outline-none focus:border-emerald-400" /></label></div><ErrorMessage message={tableError} />{tableLoading ? <Loading label={`Loading ${tab.toLowerCase()} statistics...`} /> : pageData?.content?.length ? <>{tab === 'Players' && <Table headers={['Player', 'Country', 'Goals', 'Assists', 'CS', 'Rating', 'Minutes']} rows={playerRows} />}{tab === 'Teams' && <Table headers={['Team', 'Matches', 'W', 'D', 'L', 'GF', 'GA', 'CS', 'Win %']} rows={teamRows} />}{tab === 'Tournaments' && <Table headers={['Tournament', 'Year', 'Status', 'Matches', 'Goals', 'Avg.', 'CS', 'Champion']} rows={tournamentRows} />}<Pagination page={pageData.number} totalPages={pageData.totalPages} totalElements={pageData.totalElements} onPageChange={setPage} /></> : <EmptyState title={`No ${tab.toLowerCase()} statistics`} description="The backend returned no records for this page or filter." />}</Card>}{tab === 'Records' && <Records records={records} />}</div>
}
