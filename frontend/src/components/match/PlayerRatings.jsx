import Card from '../common/Card'
import EmptyState from '../common/EmptyState'

const ratingClass = (rating) => {
  if (rating == null) return 'bg-slate-800 text-slate-300'
  if (rating >= 8) return 'bg-emerald-400/15 text-emerald-200'
  if (rating >= 7) return 'bg-sky-400/15 text-sky-200'
  if (rating < 6) return 'bg-rose-400/15 text-rose-200'
  return 'bg-slate-800 text-slate-200'
}

function RatingList({ team, ratings, manOfTheMatchId }) {
  return <section><h3 className="mb-3 truncate text-base font-bold text-slate-100">{team}</h3>{ratings.length ? <div className="space-y-2">{ratings.map((rating) => <div key={rating.playerId} className="flex items-center justify-between gap-3 rounded-lg border border-slate-800 bg-slate-950/50 p-3"><div className="min-w-0"><p className="truncate font-semibold text-slate-100">{rating.playerName}{rating.playerId === manOfTheMatchId && <span className="ml-2 text-xs text-amber-300">★ POTM</span>}</p><p className="mt-1 text-xs font-bold uppercase tracking-wide text-slate-500">{rating.position || 'Position unavailable'}</p></div><span className={`rounded-lg px-2.5 py-1 font-bold ${ratingClass(rating.rating)}`}>{rating.rating == null ? '—' : rating.rating.toFixed(2)}</span></div>)}</div> : <EmptyState title="No ratings" description="No persisted ratings for this team." />}</section>
}

export default function PlayerRatings({ ratings, homeTeam, awayTeam, manOfTheMatch }) {
  if (!ratings?.length) return <EmptyState title="Player ratings unavailable" description="The backend has not persisted player ratings for this match." />
  const sortRatings = (team) => ratings.filter((rating) => rating.team === team).sort((first, second) => (second.rating ?? -Infinity) - (first.rating ?? -Infinity))
  return <Card><h2 className="text-xl font-bold text-white">Player ratings</h2><p className="mt-1 text-sm text-slate-400">Persisted match ratings, ordered by rating within each team.</p><div className="mt-5 grid gap-6 lg:grid-cols-2"><RatingList team={homeTeam} ratings={sortRatings(homeTeam)} manOfTheMatchId={manOfTheMatch?.playerId} /><RatingList team={awayTeam} ratings={sortRatings(awayTeam)} manOfTheMatchId={manOfTheMatch?.playerId} /></div></Card>
}
