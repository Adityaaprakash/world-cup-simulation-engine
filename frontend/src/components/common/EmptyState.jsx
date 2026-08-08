export default function EmptyState({ title, description }) {
  return <div className="rounded-xl border border-dashed border-slate-700 p-6 text-center"><h3 className="font-semibold text-slate-100">{title}</h3><p className="mt-2 text-sm text-slate-400">{description}</p></div>
}
