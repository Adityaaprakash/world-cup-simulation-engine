import axiosClient from './axiosClient'

export const getTournamentStatistics = (name, year) => axiosClient.get('/api/statistics/tournaments', {
  params: { name, year, size: 50 },
})

export const getPlayerStatistics = (params) => axiosClient.get('/api/statistics/players', { params })
export const getTeamStatistics = (params) => axiosClient.get('/api/statistics/teams', { params })
export const getPagedTournamentStatistics = (params) => axiosClient.get('/api/statistics/tournaments', { params })
export const getFootballRecords = () => axiosClient.get('/api/statistics/records')
export const getStatisticsSummary = () => axiosClient.get('/api/statistics/summary')

export const getTopScorers = () => axiosClient.get('/api/optimization/leaderboards/scorers')
export const getTopAssists = () => axiosClient.get('/api/optimization/leaderboards/assists')
export const getTopCleanSheets = () => axiosClient.get('/api/optimization/leaderboards/cleansheets')
export const getTopRatings = () => axiosClient.get('/api/optimization/leaderboards/ratings')
