export default function ErrorMessage({ message }) {
  if (!message) return null

  return <p role="alert" className="rounded-lg border border-rose-500/40 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">{message}</p>
}
