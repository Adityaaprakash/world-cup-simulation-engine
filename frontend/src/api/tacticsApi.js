import axiosClient from './axiosClient'

export const getTeamTactics = (teamId) => axiosClient.get(`/api/v1/teams/${teamId}/tactics`)
export const updateTeamTactics = (teamId, tacticsRequest) => axiosClient.put(`/api/v1/teams/${teamId}/tactics`, tacticsRequest)
