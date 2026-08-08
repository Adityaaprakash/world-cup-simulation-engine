export default function Loading({ label = 'Loading...' }) {
  return <div className="flex items-center gap-3 p-6 text-slate-300"><span className="h-5 w-5 animate-spin rounded-full border-2 border-emerald-400 border-t-transparent" />{label}</div>
}
