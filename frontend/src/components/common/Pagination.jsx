import Button from './Button'

export default function Pagination({ page, totalPages, totalElements, onPageChange }) {
  if (!totalPages) return null
  return <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-slate-400"><span>Page {page + 1} of {totalPages} · {totalElements} total</span><div className="flex gap-2"><Button variant="secondary" className="px-3 py-1.5 text-sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>Previous</Button><Button variant="secondary" className="px-3 py-1.5 text-sm" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>Next</Button></div></div>
}
