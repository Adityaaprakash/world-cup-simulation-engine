import React, { useState, useEffect } from 'react';
import api from '../api/axiosClient';

export default function ManagerEconomy() {
    const [economy, setEconomy] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);
    const [successMessage, setSuccessMessage] = useState('');

    const [allocations, setAllocations] = useState({
        trainingAllocation: 0,
        medicalAllocation: 0,
        scoutingAllocation: 0
    });

    useEffect(() => {
        fetchEconomy();
    }, []);

    const fetchEconomy = async () => {
        setLoading(true);
        setError('');
        try {
            const response = await api.get('/manager/economy');
            setEconomy(response.data);
            setAllocations({
                trainingAllocation: response.data.trainingAllocation || 0,
                medicalAllocation: response.data.medicalAllocation || 0,
                scoutingAllocation: response.data.scoutingAllocation || 0,
            });
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to fetch economy data');
        } finally {
            setLoading(false);
        }
    };

    const handleAllocationChange = (field, value) => {
        setAllocations(prev => ({
            ...prev,
            [field]: parseInt(value) || 0
        }));
    };

    const handleSaveAllocations = async () => {
        const total = allocations.trainingAllocation + allocations.medicalAllocation + allocations.scoutingAllocation;
        if (total > economy.balance) {
            setError('Allocations exceed total balance');
            return;
        }

        setSaving(true);
        setError('');
        setSuccessMessage('');
        try {
            await api.post('/manager/economy/allocate', allocations);
            setSuccessMessage('Allocations updated successfully');
            await fetchEconomy();
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to update allocations');
        } finally {
            setSaving(false);
        }
    };

    if (loading) return <div className="p-4">Loading economy data...</div>;

    const totalAllocated = allocations.trainingAllocation + allocations.medicalAllocation + allocations.scoutingAllocation;
    const remaining = economy.balance - totalAllocated;

    return (
        <div className="p-4 max-w-4xl mx-auto">
            <h1 className="text-2xl font-bold mb-6 text-gray-100">Manager Federation Economy</h1>
            
            {error && <div className="bg-red-900 border border-red-500 text-red-100 px-4 py-3 rounded mb-4">{error}</div>}
            {successMessage && <div className="bg-green-900 border border-green-500 text-green-100 px-4 py-3 rounded mb-4">{successMessage}</div>}
            
            <div className="bg-gray-800 rounded-lg p-6 mb-8 border border-gray-700">
                <div className="text-xl font-semibold mb-2 text-gray-200">Current Balance</div>
                <div className="text-4xl font-bold text-green-400">${economy.balance?.toLocaleString()}</div>
            </div>

            <div className="bg-gray-800 rounded-lg p-6 mb-8 border border-gray-700">
                <h2 className="text-xl font-semibold mb-4 text-gray-200">Resource Allocation</h2>
                
                <div className="mb-4">
                    <div className="flex justify-between mb-2">
                        <span className="text-gray-300">Remaining Balance to Allocate:</span>
                        <span className={remaining < 0 ? 'text-red-400 font-bold' : 'text-gray-200 font-bold'}>
                            ${remaining.toLocaleString()}
                        </span>
                    </div>
                </div>

                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1">
                            Training & Development
                        </label>
                        <input
                            type="number"
                            min="0"
                            className="bg-gray-700 border border-gray-600 text-gray-100 text-sm rounded focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5"
                            value={allocations.trainingAllocation}
                            onChange={(e) => handleAllocationChange('trainingAllocation', e.target.value)}
                        />
                        <p className="text-xs text-gray-500 mt-1">Boosts player development and helps maintain form.</p>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1">
                            Medical & Recovery
                        </label>
                        <input
                            type="number"
                            min="0"
                            className="bg-gray-700 border border-gray-600 text-gray-100 text-sm rounded focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5"
                            value={allocations.medicalAllocation}
                            onChange={(e) => handleAllocationChange('medicalAllocation', e.target.value)}
                        />
                        <p className="text-xs text-gray-500 mt-1">Improves player fitness recovery between matches and reduces injury duration.</p>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1">
                            Scouting & Tactics
                        </label>
                        <input
                            type="number"
                            min="0"
                            className="bg-gray-700 border border-gray-600 text-gray-100 text-sm rounded focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5"
                            value={allocations.scoutingAllocation}
                            onChange={(e) => handleAllocationChange('scoutingAllocation', e.target.value)}
                        />
                        <p className="text-xs text-gray-500 mt-1">Increases tactical familiarity and exposes opponent weaknesses.</p>
                    </div>
                </div>

                <div className="mt-6 flex justify-end">
                    <button
                        onClick={handleSaveAllocations}
                        disabled={saving || remaining < 0}
                        className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-6 rounded disabled:opacity-50"
                    >
                        {saving ? 'Saving...' : 'Update Allocations'}
                    </button>
                </div>
            </div>

            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
                <h2 className="text-xl font-semibold mb-4 text-gray-200">Recent Transactions</h2>
                
                {economy.recentTransactions?.length === 0 ? (
                    <p className="text-gray-400">No transactions recorded yet.</p>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm text-left text-gray-300">
                            <thead className="text-xs text-gray-400 uppercase bg-gray-700">
                                <tr>
                                    <th className="px-4 py-3">Date</th>
                                    <th className="px-4 py-3">Amount</th>
                                    <th className="px-4 py-3">Reason</th>
                                </tr>
                            </thead>
                            <tbody>
                                {economy.recentTransactions?.map((tx, idx) => (
                                    <tr key={idx} className="border-b border-gray-700 hover:bg-gray-700/50">
                                        <td className="px-4 py-3">{new Date(tx.date).toLocaleDateString()}</td>
                                        <td className={`px-4 py-3 font-semibold ${tx.amount > 0 ? 'text-green-400' : 'text-red-400'}`}>
                                            {tx.amount > 0 ? '+' : ''}{tx.amount}
                                        </td>
                                        <td className="px-4 py-3">{tx.reason}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}
