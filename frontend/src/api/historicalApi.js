import axiosClient from './axiosClient'

export const getPlayerLegacy = (params) => axiosClient.get('/api/history/players', { params })
export const getTeamLegacy = (params) => axiosClient.get('/api/history/teams', { params })
export const getManagerLegacy = (params) => axiosClient.get('/api/history/managers', { params })
export const getHallOfFame = () => axiosClient.get('/api/history/hall-of-fame')
export const getRivalries = (type) => axiosClient.get('/api/history/rivalries', { params: { type } })
export const getHeadToHead = (type, firstId, secondId) => axiosClient.get('/api/history/head-to-head', { params: { type, firstId, secondId } })
export const getEraAnalysis = () => axiosClient.get('/api/history/eras')
export const getHistoricalTimeline = () => axiosClient.get('/api/history/timeline')
export const getGlobalRankings = () => axiosClient.get('/api/history/rankings')
export const getHistoricalSummary = () => axiosClient.get('/api/history/summary')
