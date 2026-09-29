import axiosClient from './axiosClient'

export const getMatchDetail = (matchId) => axiosClient.get(`/api/matches/${matchId}`)
export const getLiveSnapshot = (matchId) => axiosClient.get(`/api/matches/${matchId}/live`)
