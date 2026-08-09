import axiosClient from './axiosClient'

export const getMySquads = () => axiosClient.get('/api/squads/my')
export const getSquadPlayers = (squadId) => axiosClient.get(`/api/squads/${squadId}/players`)
export const addSquadPlayer = (squadId, playerId) => axiosClient.post(`/api/squads/${squadId}/players`, { playerId })
export const removeSquadPlayer = (squadId, playerId) => axiosClient.delete(`/api/squads/${squadId}/players/${playerId}`)
export const getLineup = (squadId) => axiosClient.get(`/api/squads/${squadId}/lineup`)
export const updateStartingXi = (squadId, playerIds) => axiosClient.put(`/api/squads/${squadId}/starting-xi`, { playerIds })
export const assignPosition = (squadId, playerId, positionSlot) => axiosClient.put(`/api/squads/${squadId}/positions`, { playerId, positionSlot })
export const setCaptain = (squadId, playerId) => axiosClient.put(`/api/squads/${squadId}/captain`, { playerId })
export const validateLineup = (squadId) => axiosClient.get(`/api/squads/${squadId}/validate`)
export const getSquadReadyStatus = (squadId) => axiosClient.get(`/api/squads/${squadId}/ready`)
