import axiosClient from './axiosClient'

export const getTeams = () => axiosClient.get('/api/teams')
export const getTeam = (teamId) => axiosClient.get(`/api/teams/${teamId}`)
export const getTeamPlayers = (teamId) => axiosClient.get(`/api/teams/${teamId}/players`)
