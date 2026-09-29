import apiClient from './client'
import type { ApiResponse, AuthResponse, CandidateProfile, UpdatePreferencesRequest, Job, JobApplication, MatchResult, PageResponse, DashboardStats, Resume, ResumeProfileFields } from '@/types'

// ─── Auth ─────────────────────────────────────────────────────────────────────
export const authApi = {
  register: (data: { name: string; email: string; password: string }) =>
    apiClient.post<ApiResponse<AuthResponse>>('/auth/register', data),

  login: (data: { email: string; password: string }) =>
    apiClient.post<ApiResponse<AuthResponse>>('/auth/login', data),
}

// ─── Profile ──────────────────────────────────────────────────────────────────
export const profileApi = {
  get: () => apiClient.get<ApiResponse<CandidateProfile>>('/profile'),

  updatePreferences: (data: UpdatePreferencesRequest) =>
    apiClient.put<ApiResponse<CandidateProfile>>('/profile/preferences', data),

  confirmSkills: (skills: { skillName: string; proficiency: string }[]) =>
    apiClient.post<ApiResponse<CandidateProfile>>('/profile/skills/confirm', skills),
}

// ─── Resume ───────────────────────────────────────────────────────────────────
export const resumeApi = {
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post('/resumes/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  extractSkills: (resumeId: string) =>
    apiClient.post(`/resumes/${resumeId}/extract-skills`),

  list: () => apiClient.get<ApiResponse<Resume[]>>('/resumes'),

  download: (resumeId: string) =>
    apiClient.get(`/resumes/${resumeId}/download`, { responseType: 'blob' }),

  getProfileFields: (resumeId: string) =>
    apiClient.get<ApiResponse<ResumeProfileFields>>(`/resumes/${resumeId}/profile-fields`),
}

// ─── Jobs ─────────────────────────────────────────────────────────────────────
export const jobsApi = {
  list: (page = 0, size = 500) =>
    apiClient.get<ApiResponse<PageResponse<Job>>>(`/jobs?page=${page}&size=${size}`),

  getById: (id: string) =>
    apiClient.get<ApiResponse<Job>>(`/jobs/${id}`),

  scan: (source?: string, keywords?: string[]) => {
    const params = new URLSearchParams()
    if (source) params.set('source', source)
    if (keywords?.length) params.set('keywords', keywords.join(','))
    const qs = params.toString()
    return apiClient.post<ApiResponse<string>>(`/jobs/scan${qs ? `?${qs}` : ''}`)
  },
}

// ─── Generator ────────────────────────────────────────────────────────────────
export const generatorApi = {
  generateResume: (jobId: string) =>
    apiClient.post<ApiResponse<{ optimizedResume: string }>>(`/generate/resume/${jobId}`),

  generateCoverLetter: (jobId: string) =>
    apiClient.post<ApiResponse<{ coverLetter: string }>>(`/generate/cover-letter/${jobId}`),
}

// ─── Matches ──────────────────────────────────────────────────────────────────
export const matchesApi = {
  getMatches: (limit = 30) =>
    apiClient.get<ApiResponse<MatchResult[]>>(`/jobs/matches?limit=${limit}`),
}

// ─── Tracker ──────────────────────────────────────────────────────────────────
export const trackerApi = {
  list: () => apiClient.get<ApiResponse<JobApplication[]>>('/tracker'),

  stats: () => apiClient.get<ApiResponse<DashboardStats>>('/tracker/stats'),

  saveJob: (jobId: string) =>
    apiClient.post<ApiResponse<JobApplication>>(`/tracker/save/${jobId}`),

  markApplied: (jobId: string) =>
    apiClient.post<ApiResponse<JobApplication>>(`/tracker/apply/${jobId}`),

  updateStatus: (applicationId: string, status: string) =>
    apiClient.patch(`/tracker/${applicationId}/status?status=${status}`),
}
