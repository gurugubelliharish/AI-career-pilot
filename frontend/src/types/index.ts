// ─── Resume ───────────────────────────────────────────────────────────────────
export interface Resume {
  id: string
  fileName: string
  fileType?: string
  filePath?: string
  processed: boolean
  active: boolean
  parsedData?: { skills?: string[]; [key: string]: unknown }
  createdAt: string
}

export interface ResumeProfileFields {
  name?: string
  email?: string
  phone?: string
  linkedinUrl?: string
  githubUrl?: string
  skills?: string[]
}

// ─── Auth ─────────────────────────────────────────────────────────────────────
export interface AuthResponse {
  token: string
  tokenType: string
  userId: string
  name: string
  email: string
}

// ─── Profile ──────────────────────────────────────────────────────────────────
export interface CandidateProfile {
  id: string
  name: string
  email: string
  phone?: string
  linkedinUrl?: string
  githubUrl?: string
  preferredRoles: string[]
  preferredLocations: string[]
  workMode?: string
  experienceLevel?: string
  expectedSalary?: string
  noticePeriod?: string
  careerGoals: string[]
  skills: CandidateSkill[]
  profileComplete: boolean
}

export interface UpdatePreferencesRequest {
  name?: string
  phone?: string
  linkedinUrl?: string
  githubUrl?: string
  preferredRoles?: string[]
  preferredLocations?: string[]
  workMode?: string
  experienceLevel?: string
  expectedSalary?: string
  noticePeriod?: string
  careerGoals?: string[]
}

export interface CandidateSkill {
  id: string
  skillName: string
  proficiency?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  inferred: boolean
  confirmed: boolean
}

// ─── Jobs ─────────────────────────────────────────────────────────────────────
export interface Job {
  id: string
  title: string
  company: string
  location?: string
  description?: string
  workMode?: string
  source: string
  sourceUrl?: string
  requiredSkills: string[]
  postedAt?: string
  freshnessScore: number
}

export interface MatchResult {
  jobId: string
  jobTitle: string
  company: string
  matchScore: number
  freshnessScore: number
  goalAlignmentScore: number
  priorityScore: number
  priority: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
  matchedSkills: string[]
  missingSkills: string[]
  postedAgo: string
}

// ─── Tracker ──────────────────────────────────────────────────────────────────
export type ApplicationStatus = 'SAVED' | 'APPLIED' | 'INTERVIEW' | 'REJECTED' | 'OFFER'

export interface JobApplication {
  id: string
  job: Job
  status: ApplicationStatus
  matchScore: number
  priority?: string
  notes?: string
  appliedAt?: string
  createdAt: string
}

export interface DashboardStats {
  total: number
  saved: number
  applied: number
  interview: number
  offer: number
}

// ─── API ──────────────────────────────────────────────────────────────────────
export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
  error?: string
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
