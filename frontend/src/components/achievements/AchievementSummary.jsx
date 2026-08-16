import Card from '../common/Card'

export default function AchievementSummary({ achievements }) {
  const unlocked = achievements?.length || 0
  return <Card className="border-emerald-400/20"><p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-300">Manager achievements</p><div className="mt-3 flex flex-wrap items-end justify-between gap-4"><div><p className="text-4xl font-bold text-white">{unlocked}</p><p className="mt-1 text-sm text-slate-400">Unlocked achievements recorded by the backend</p></div><p className="max-w-sm text-sm text-slate-400">The backend currently exposes unlocked achievements only; locked achievements and progress are not shown.</p></div></Card>
}
