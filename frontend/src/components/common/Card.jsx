export default function Card({ children, className = '' }) {
  return <section className={`rounded-xl border border-slate-800 bg-slate-900/80 p-5 shadow-lg shadow-slate-950/20 ${className}`}>{children}</section>
}
