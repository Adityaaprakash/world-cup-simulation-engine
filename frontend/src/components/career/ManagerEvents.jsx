import React, { useEffect, useState } from 'react'
import { getManagerEvents, getPendingEvents, makeDecision } from '../../api/managerApi'
import Card from '../common/Card'
import EmptyState from '../common/EmptyState'
import ErrorMessage from '../common/ErrorMessage'
import Loading from '../common/Loading'

function EventCard({ event, onDecisionMade }) {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  
  const handleDecision = async (decisionCode) => {
    setSubmitting(true)
    setError(null)
    try {
      const response = await makeDecision(event.id, decisionCode)
      onDecisionMade(response.data)
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to submit decision.')
    } finally {
      setSubmitting(false)
    }
  }

  const isPending = event.status === 'PENDING'
  const isExpired = event.status === 'EXPIRED'
  const isResolved = event.status === 'RESOLVED'

  return (
    <Card className="p-4 mb-4 border-l-4 border-l-blue-500">
      <div className="flex justify-between items-start mb-2">
        <h3 className="text-lg font-bold text-slate-100">{event.title}</h3>
        <span className={`px-2 py-1 text-xs font-semibold rounded-full ${isPending ? 'bg-amber-500/20 text-amber-300' : isResolved ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-700 text-slate-400'}`}>
          {event.status}
        </span>
      </div>
      
      <p className="text-slate-300 mb-4">{event.description}</p>
      
      {error && <ErrorMessage message={error} />}
      
      {isPending && event.options && event.options.length > 0 && (
        <div className="space-y-2 mt-4">
          <p className="text-sm font-semibold text-slate-400 mb-2">Available Options:</p>
          <div className="grid gap-2 grid-cols-1 sm:grid-cols-2">
             {event.options.map((opt) => (
               <button
                 key={opt.code}
                 onClick={() => handleDecision(opt.code)}
                 disabled={submitting}
                 className="flex flex-col text-left p-3 rounded-md bg-slate-800 hover:bg-slate-700 disabled:opacity-50 border border-slate-700 transition"
               >
                 <span className="font-semibold text-blue-400">{opt.title}</span>
                 <span className="text-sm text-slate-400 mt-1">{opt.consequenceDescription}</span>
               </button>
             ))}
          </div>
        </div>
      )}
      
      {submitting && <div className="mt-4"><Loading label="Submitting decision..." /></div>}
      
      {isResolved && (
        <div className="mt-4 p-3 bg-slate-800/50 rounded-md border border-slate-700">
          <p className="text-sm font-semibold text-emerald-400">Resolution:</p>
          <p className="text-sm text-slate-300 mt-1">{event.resolutionText}</p>
        </div>
      )}
      
      {!isPending && !isResolved && (
        <div className="mt-4 p-3 bg-slate-800/50 rounded-md border border-slate-700">
           <p className="text-sm text-slate-400">This event is no longer actionable.</p>
        </div>
      )}
    </Card>
  )
}

export default function ManagerEvents({ initialEvents = null }) {
  const [events, setEvents] = useState(initialEvents)
  const [loading, setLoading] = useState(!initialEvents)
  const [error, setError] = useState(null)

  const fetchEvents = async () => {
    try {
      setLoading(true)
      const res = await getManagerEvents()
      setEvents(res.data)
      setError(null)
    } catch (err) {
      setError(err.message || 'Failed to load events.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!initialEvents) {
      fetchEvents()
    }
  }, [initialEvents])

  const handleDecisionMade = (updatedEvent) => {
    setEvents(current => current.map(e => e.id === updatedEvent.id ? updatedEvent : e))
  }

  if (loading) {
    return <Loading label="Loading your events..." />
  }

  if (error) {
    return <ErrorMessage message={error} />
  }

  if (!events || events.length === 0) {
    return (
      <EmptyState 
        title="No Events" 
        description="No decisions require your attention at this time." 
      />
    )
  }

  const pendingEvents = events.filter(e => e.status === 'PENDING')
  const historyEvents = events.filter(e => e.status !== 'PENDING')

  return (
    <div className="space-y-6">
      {pendingEvents.length > 0 && (
        <section>
          <h2 className="text-xl font-bold text-slate-200 mb-4">Pending Decisions</h2>
          {pendingEvents.map(event => (
            <EventCard key={event.id} event={event} onDecisionMade={handleDecisionMade} />
          ))}
        </section>
      )}
      
      {historyEvents.length > 0 && (
        <section>
          <h2 className="text-xl font-bold text-slate-400 mb-4">Past Events</h2>
          <div className="opacity-75">
            {historyEvents.map(event => (
              <EventCard key={event.id} event={event} onDecisionMade={handleDecisionMade} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
