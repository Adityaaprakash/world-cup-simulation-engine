export default function Button({ children, className = '', variant = 'primary', type = 'button', ...props }) {
  const variants = {
    primary: 'bg-emerald-500 text-slate-950 hover:bg-emerald-400 focus:ring-emerald-400',
    secondary: 'bg-slate-800 text-slate-100 hover:bg-slate-700 focus:ring-slate-500',
    danger: 'bg-rose-500 text-white hover:bg-rose-400 focus:ring-rose-400',
  }

  return (
    <button
      type={type}
      className={`rounded-lg px-4 py-2 font-semibold transition focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-slate-950 disabled:cursor-not-allowed disabled:opacity-60 ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
