import axiosClient from './axiosClient'

export const getCurrentManager = () => axiosClient.get('/api/managers/me')
export const getCareerStatistics = () => axiosClient.get('/api/managers/me/statistics')
export const getCareerHistory = () => axiosClient.get('/api/managers/me/history')
export const getAchievements = () => axiosClient.get('/api/managers/me/achievements')
export const getCareerAnalytics = () => axiosClient.get('/api/managers/me/analytics')
export const getCareerTimeline = () => axiosClient.get('/api/managers/me/timeline')
