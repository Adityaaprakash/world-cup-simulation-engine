import { Link } from 'react-router-dom'
import Button from '../common/Button'
import Card from '../common/Card'

const formatDate = (value) => value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'
const display = (value) => value == null || value === '' ? '—' : value

function Detail({ label, value }) {
  return <div><dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</dt><dd className="mt-1 text-sm text-slate-200">{display(value)}</dd></div>
}

export default function SaveCard({ save, processing, onResume, onActivate, onEdit, onDelete, onExport, onBackup }) {
  const autosave = Boolean(save.autosave) || save.saveType === 'AUTOSAVE'
  const busy = Boolean(processing)

  return <Card className={`relative ${save.active ? 'border-emerald-400/60 ring-1 ring-emerald-400/30' : ''}`}>
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div><div className="flex flex-wrap items-center gap-2"><span className={`rounded-full px-2 py-1 text-xs font-bold ${autosave ? 'bg-sky-400/15 text-sky-200' : 'bg-slate-800 text-slate-300'}`}>{autosave ? 'AUTOSAVE' : 'MANUAL SAVE'}</span>{save.active && <span className="rounded-full bg-emerald-400/15 px-2 py-1 text-xs font-bold text-emerald-300">ACTIVE SAVE</span>}</div><h2 className="mt-3 text-xl font-bold text-white">{save.slotName}</h2><p className="mt-1 text-sm text-slate-400">Slot {save.slotNumber} · Last saved {formatDate(save.latestSaveTimestamp)}</p></div>
      {save.backupAvailable && <span className="text-xs font-semibold text-amber-300">Backup available</span>}
    </div>
    {save.description && <p className="mt-4 text-sm text-slate-300">{save.description}</p>}
    <dl className="mt-5 grid grid-cols-2 gap-x-4 gap-y-4 sm:grid-cols-3"><Detail label="Level" value={save.managerLevel} /><Detail label="XP" value={save.managerExperiencePoints} /><Detail label="Reputation" value={save.reputation} /><Detail label="Trophies" value={save.trophies} /><Detail label="Team" value={save.currentTeam} /><Detail label="Tournament" value={save.currentTournament} /><Detail label="Stage" value={save.currentStage} /><Detail label="Progress" value={save.progressPercentage == null ? null : `${save.progressPercentage}%`} /><Detail label="Season" value={save.currentSeason} /></dl>
    {save.currentTournamentId != null && <Link to={`/tournaments/${save.currentTournamentId}`} className="mt-4 inline-block text-sm font-semibold text-emerald-300 hover:text-emerald-200">View tournament</Link>}
    <div className="mt-5 flex flex-wrap gap-2"><Button disabled={busy} onClick={() => onResume(save)}>Resume</Button>{!save.active && <Button variant="secondary" disabled={busy} onClick={() => onActivate(save)}>Activate</Button>}{!autosave && <Button variant="secondary" disabled={busy} onClick={() => onEdit(save)}>Update</Button>}<Button variant="secondary" disabled={busy} onClick={() => onExport(save)}>Export</Button><Button variant="secondary" disabled={busy} onClick={() => onBackup(save)}>Backup</Button><Button variant="danger" disabled={busy} onClick={() => onDelete(save)}>Delete</Button></div>
  </Card>
}
