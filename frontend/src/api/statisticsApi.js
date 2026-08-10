import axiosClient from './axiosClient'

export const getTournamentStatistics = (name, year) => axiosClient.get('/api/statistics/tournaments', {
  params: { name, year, size: 50 },
})
