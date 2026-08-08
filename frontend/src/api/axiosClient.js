import axios from 'axios'

export const TOKEN_STORAGE_KEY = 'world-cup-auth-token'

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
})

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const defaultMessages = {
      400: 'Please check the information you entered.',
      401: 'Your session has expired. Please sign in again.',
      403: 'You do not have permission to perform this action.',
      404: 'The requested resource could not be found.',
      409: 'This action conflicts with the current application state.',
      500: 'The server encountered an error. Please try again later.',
    }
    const message = error.response?.data?.message
      || defaultMessages[status]
      || (error.request ? 'Unable to reach the server. Check your connection and try again.' : error.message)
      || 'Something went wrong. Please try again.'

    if (status === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      window.dispatchEvent(new Event('auth:unauthorized'))
    }

    return Promise.reject({ status, message, data: error.response?.data })
  },
)

export default axiosClient
