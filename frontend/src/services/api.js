import axios from 'axios'

// VITE_API_BASE_URL should be the backend origin (no trailing slash, no /api).
// Local dev:  set to http://localhost:8080 in .env  (proxy not needed)
// Production: set to https://your-app.onrender.com in Vercel env vars
const baseURL = `${(import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')}/api`

const api = axios.create({
  baseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('soulsync_token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    config.headers['X-Tenant-ID'] = window.__TENANT_ID__ ?? 'soulsync'
    const profileId = localStorage.getItem('soulsync_profile_id')
    if (profileId) config.headers['X-Profile-Id'] = profileId

    // For FormData (file uploads) let axios/browser set Content-Type with
    // the correct multipart boundary — never force application/json.
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'An unexpected error occurred'
    return Promise.reject(new Error(message))
  }
)

export default api
