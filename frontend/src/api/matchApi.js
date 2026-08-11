import axiosClient from './axiosClient'

export const getMatchDetail = (matchId) => axiosClient.get(`/api/matches/${matchId}`)
