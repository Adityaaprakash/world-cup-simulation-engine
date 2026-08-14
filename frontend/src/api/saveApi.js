import axiosClient from './axiosClient'

export const getSaves = () => axiosClient.get('/api/saves')
export const getSave = (id) => axiosClient.get(`/api/saves/${id}`)
export const createSave = (payload) => axiosClient.post('/api/saves', payload)
export const updateSave = (id, payload) => axiosClient.put(`/api/saves/${id}`, payload)
export const deleteSave = (id) => axiosClient.delete(`/api/saves/${id}`)
export const createAutosave = () => axiosClient.post('/api/saves/autosave')
export const activateSave = (id) => axiosClient.post(`/api/saves/${id}/activate`)
export const resumeSave = (id) => axiosClient.post(`/api/saves/${id}/resume`)
export const exportSave = (id) => axiosClient.get(`/api/saves/${id}/export`)
export const importSave = (payload) => axiosClient.post('/api/saves/import', payload)
export const backupSave = (id) => axiosClient.post(`/api/saves/${id}/backup`)
