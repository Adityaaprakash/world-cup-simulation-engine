import React, { useEffect, useState } from 'react'
import Card from '../components/common/Card'
import Loading from '../components/common/Loading'
import ErrorMessage from '../components/common/ErrorMessage'
import Button from '../components/common/Button'
import { getContracts, getExpiringContracts, renewContract, terminateContract } from '../api/contractApi'
import { getPlayerDetails } from '../api/playerApi'

export default function Contracts() {
  const [contracts, setContracts] = useState([])
  const [expiring, setExpiring] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [playerCache, setPlayerCache] = useState({})

  const fetchContracts = async () => {
    setIsLoading(true)
    try {
      const res = await getContracts()
      const expiringRes = await getExpiringContracts()
      setContracts(res)
      setExpiring(expiringRes)
      
      const pCache = {}
      for (const contract of res) {
          if (!pCache[contract.playerId]) {
              const playerRes = await getPlayerDetails(contract.playerId)
              pCache[contract.playerId] = playerRes.data
          }
      }
      setPlayerCache(pCache)
    } catch (err) {
      setError('Failed to fetch contracts.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchContracts()
  }, [])

  const handleRenew = async (id) => {
    try {
      await renewContract(id)
      fetchContracts()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to renew contract.')
    }
  }

  const handleTerminate = async (id) => {
    try {
      await terminateContract(id)
      fetchContracts()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to terminate contract.')
    }
  }
  
  if (isLoading) return <Loading label="Loading contracts..." />

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold text-white">Contract Management</h1>
      <ErrorMessage message={error} />
      
      {expiring.length > 0 && (
          <Card className="border-l-4 border-l-amber-500">
             <h2 className="text-xl font-bold text-amber-500">Expiring Contracts ({expiring.length})</h2>
             <p className="text-slate-400 mb-4">The following players have expiring contracts.</p>
             <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                 {expiring.map(contract => {
                     const player = playerCache[contract.playerId];
                     return (
                         <div key={contract.id} className="rounded-lg bg-slate-800 p-4 border border-amber-900 border-opacity-50">
                             <div className="flex justify-between items-start">
                                 <div>
                                     <h3 className="font-bold text-white">{player?.name || 'Unknown Player'}</h3>
                                     <p className="text-sm text-slate-400">{contract.status} (Expires: Season {contract.expirySeason})</p>
                                 </div>
                             </div>
                             <div className="mt-4 flex gap-2">
                                 <Button onClick={() => handleRenew(contract.id)} className="bg-emerald-600 hover:bg-emerald-500 text-xs">Renew</Button>
                                 <Button onClick={() => handleTerminate(contract.id)} className="bg-red-600 hover:bg-red-500 text-xs">Terminate</Button>
                             </div>
                         </div>
                     )
                 })}
             </div>
          </Card>
      )}

      <Card>
        <h2 className="text-xl font-bold text-white mb-4">All Active Contracts</h2>
        {contracts.length === 0 ? (
            <p className="text-slate-400">No active contracts found.</p>
        ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-400">
                <thead className="bg-slate-900/50 text-xs uppercase text-slate-300">
                  <tr>
                    <th className="px-4 py-3">Player</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3">Valid From</th>
                    <th className="px-4 py-3">Expires</th>
                    <th className="px-4 py-3">Renewals</th>
                    <th className="px-4 py-3">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {contracts.map(contract => (
                    <tr key={contract.id} className="hover:bg-slate-800/50">
                      <td className="px-4 py-3 font-semibold text-white">
                        {playerCache[contract.playerId]?.name || `Player #${contract.playerId}`}
                      </td>
                      <td className="px-4 py-3">
                          <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                              contract.status === 'ACTIVE' ? 'bg-emerald-400/10 text-emerald-400' :
                              contract.status === 'EXPIRING' ? 'bg-amber-400/10 text-amber-400' :
                              'bg-rose-400/10 text-rose-400'
                          }`}>
                            {contract.status}
                          </span>
                      </td>
                      <td className="px-4 py-3">Season {contract.startSeason}</td>
                      <td className="px-4 py-3">
                        Season {contract.expirySeason}
                      </td>
                      <td className="px-4 py-3 px-4">{contract.renewalCount}</td>
                      <td className="px-4 py-3 flex gap-2">
                          {(contract.status === 'ACTIVE' || contract.status === 'EXPIRING') && (
                              <>
                                <button onClick={() => handleRenew(contract.id)} className="text-emerald-400 hover:text-emerald-300">Renew</button>
                                <button onClick={() => handleTerminate(contract.id)} className="text-rose-400 hover:text-rose-300">Terminate</button>
                              </>
                          )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
        )}
      </Card>
    </div>
  )
}
