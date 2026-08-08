import axiosClient from './axiosClient'

export const loginRequest = (credentials) => axiosClient.post('/api/auth/login', credentials)

export const registerRequest = (details) => axiosClient.post('/api/auth/register', details)
