import axiosClient from './axiosClient'

export const searchPlayers = (data) => axiosClient.post('/api/search/players', data)
export const getPlayerDetails = (id) => axiosClient.get(`/api/players/${id}`)
export const comparePlayers = (playerIds) => axiosClient.post('/api/players/compare', { playerIds })
