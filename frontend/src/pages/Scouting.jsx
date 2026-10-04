import { useState, useEffect } from 'react';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Loading from '../components/common/Loading';
import ErrorMessage from '../components/common/ErrorMessage';
import * as scoutingApi from '../api/scoutingApi';

const SearchIcon = () => (
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
);

const UserIcon = () => (
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
);

const RadarIcon = () => (
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"></path>
  </svg>
);

export default function Scouting() {
  const [scouts, setScouts] = useState([]);
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('Network');

  const fetchData = async () => {
    try {
      setLoading(true);
      const [sData, rData] = await Promise.all([
        scoutingApi.getScouts(),
        scoutingApi.getReports()
      ]);
      setScouts(sData);
      setReports(rData);
    } catch (err) {
      setError('Failed to load scouting data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSimulateAdvance = async () => {
    try {
      await scoutingApi.simulateAdvance(15);
      await fetchData();
    } catch (err) {
      // Ignored for demo
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight flex items-center gap-3">
            <span className="p-2 bg-emerald-500/20 text-emerald-400 rounded-lg">
              <RadarIcon />
            </span>
            Scouting Hub
          </h1>
          <p className="text-slate-400 mt-1">Manage your scout network and unearth the next global superstar.</p>
        </div>
        <div className="flex gap-2">
          <Button onClick={handleSimulateAdvance} variant="secondary">Advance 15 Days</Button>
          <Button variant="primary">Hire Scout</Button>
        </div>
      </div>

      <div className="flex gap-1 p-1 bg-slate-900/50 backdrop-blur rounded-xl border border-slate-800 w-max">
        {['Network', 'Target Reports'].map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-6 py-2.5 rounded-lg text-sm font-semibold transition-all duration-300 ${
              activeTab === tab
                ? 'bg-emerald-500 text-slate-950 shadow-lg shadow-emerald-500/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {activeTab === 'Network' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {scouts.length === 0 ? (
            <div className="col-span-full py-12 text-center text-slate-500 border border-dashed border-slate-800 rounded-2xl">
              <UserIcon className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p>You haven't hired any scouts yet.</p>
            </div>
          ) : scouts.map((scout) => (
            <div key={scout.id} className="group relative bg-slate-900/40 hover:bg-slate-800/60 transition-colors border border-slate-800 rounded-2xl p-6 overflow-hidden">
              <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/5 rounded-full blur-2xl -mr-10 -mt-10 group-hover:bg-emerald-500/10 transition-colors"></div>
              
              <div className="flex justify-between items-start mb-6">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-full bg-gradient-to-br from-slate-700 to-slate-800 border-2 border-slate-700 flex items-center justify-center">
                    <UserIcon />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-200">{scout.name}</h3>
                    <p className="text-sm font-medium text-emerald-400 capitalize">{scout.regionSpecialization} Expert</p>
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-slate-400 uppercase font-semibold tracking-wider">Evaluation</span>
                    <span className="text-slate-200 font-medium">{scout.evaluationSkill}</span>
                  </div>
                  <div className="h-1.5 bg-slate-800 rounded-full overflow-hidden">
                    <div className="h-full bg-sky-500 rounded-full" style={{ width: `${scout.evaluationSkill}%`}}></div>
                  </div>
                </div>
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-slate-400 uppercase font-semibold tracking-wider">Potential Eval</span>
                    <span className="text-slate-200 font-medium">{scout.potentialEvaluationSkill}</span>
                  </div>
                  <div className="h-1.5 bg-slate-800 rounded-full overflow-hidden">
                    <div className="h-full bg-fuchsia-500 rounded-full" style={{ width: `${scout.potentialEvaluationSkill}%`}}></div>
                  </div>
                </div>
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-slate-400 uppercase font-semibold tracking-wider">Tactical</span>
                    <span className="text-slate-200 font-medium">{scout.tacticalKnowledge}</span>
                  </div>
                  <div className="h-1.5 bg-slate-800 rounded-full overflow-hidden">
                    <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${scout.tacticalKnowledge}%`}}></div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {activeTab === 'Target Reports' && (
        <div className="bg-slate-900/40 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl">
          {reports.length === 0 ? (
            <div className="py-16 text-center text-slate-500">
              <SearchIcon className="w-12 h-12 mx-auto mb-4 opacity-50" />
              <p className="text-lg">No active or completed reports</p>
              <p className="text-sm mt-1">Assign a scout to a player to begin generating data.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-800 text-xs uppercase tracking-wider text-slate-400 bg-slate-900/80">
                    <th className="p-4 font-semibold">Player</th>
                    <th className="p-4 font-semibold">Scout</th>
                    <th className="p-4 font-semibold">Status</th>
                    <th className="p-4 font-semibold">OVR (Est)</th>
                    <th className="p-4 font-semibold">POT (Est)</th>
                    <th className="p-4 font-semibold text-right">Recommendation</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {reports.map((report) => (
                    <tr key={report.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="p-4">
                        <div className="font-bold text-slate-200">{report.playerName || `Player #${report.playerId}`}</div>
                        <div className="text-xs text-slate-500">ID: {report.playerId}</div>
                      </td>
                      <td className="p-4 text-sm text-slate-300">
                        {report.scoutName}
                      </td>
                      <td className="p-4">
                        {report.status === 'COMPLETED' ? (
                          <span className="px-2 py-1 rounded bg-emerald-500/10 text-emerald-400 text-xs font-bold border border-emerald-500/20">
                            COMPLETED
                          </span>
                        ) : (
                          <div className="flex items-center gap-3">
                            <span className="text-xs font-semibold text-amber-400 tracking-wide">SCOUTING</span>
                            <div className="w-16 h-1.5 bg-slate-800 rounded-full overflow-hidden">
                              <div className="h-full bg-amber-400 rounded-full" style={{ width: `${report.progress || 0}%`}}></div>
                            </div>
                          </div>
                        )}
                      </td>
                      <td className="p-4">
                        {report.estimatedOverallMin !== null ? (
                          <div className="flex items-center gap-1.5 font-mono text-sm">
                            <span className="text-sky-400">{report.estimatedOverallMin}</span>
                            <span className="text-slate-600">-</span>
                            <span className="text-sky-500 font-bold">{report.estimatedOverallMax}</span>
                          </div>
                        ) : <span className="text-slate-600 italic text-sm">Unknown</span>}
                      </td>
                      <td className="p-4">
                        {report.estimatedPotentialMin !== null ? (
                          <div className="flex items-center gap-1.5 font-mono text-sm">
                            <span className="text-fuchsia-400">{report.estimatedPotentialMin}</span>
                            <span className="text-slate-600">-</span>
                            <span className="text-fuchsia-500 font-bold">{report.estimatedPotentialMax}</span>
                          </div>
                        ) : <span className="text-slate-600 italic text-sm">Unknown</span>}
                      </td>
                      <td className="p-4 text-right">
                        {report.recommendation ? (
                          <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold ${
                            report.recommendation.includes('HIGHLY') ? 'bg-emerald-500 text-emerald-950' : 
                            report.recommendation.includes('NOT') ? 'bg-red-500/10 text-red-400 border border-red-500/20' :
                            'bg-sky-500/10 text-sky-400 border border-sky-500/20'
                          }`}>
                            {report.recommendation.replace('_', ' ')}
                          </span>
                        ) : (
                          <span className="text-slate-600 italic text-sm">Pending</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
