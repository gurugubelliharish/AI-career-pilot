import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { profileApi, resumeApi, jobsApi } from '@/api/endpoints'
import type { CandidateProfile, CandidateSkill, UpdatePreferencesRequest, Resume, ResumeProfileFields } from '@/types'

export interface SkillConfirmPayload {
  skillName: string
  proficiency: string
}

export interface ProfileViewModel {
  profile: CandidateProfile | undefined
  resumes: Resume[]
  pendingSkills: CandidateSkill[]
  confirmedSkills: CandidateSkill[]
  isLoading: boolean
  isConfirming: boolean
  isSaving: boolean
  isApplyingResume: boolean
  confirmSkills: (skills: SkillConfirmPayload[]) => void
  updatePreferences: (data: UpdatePreferencesRequest) => void
  applyResume: (resumeId: string) => Promise<ResumeProfileFields>
}

export function useProfileViewModel(): ProfileViewModel {
  const qc = useQueryClient()
  const [applyingResume, setApplyingResume] = useState(false)

  const { data: profile, isLoading } = useQuery({
    queryKey: ['profile'],
    queryFn: () => profileApi.get().then((r) => r.data.data),
  })

  const { data: resumes = [] } = useQuery({
    queryKey: ['resumes'],
    queryFn: () => resumeApi.list().then((r) => r.data.data ?? []),
  })

  const confirmMutation = useMutation({
    mutationFn: (skills: SkillConfirmPayload[]) => profileApi.confirmSkills(skills),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['profile'] }),
  })

  const updateMutation = useMutation({
    mutationFn: (data: UpdatePreferencesRequest) => profileApi.updatePreferences(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['profile'] })
      qc.invalidateQueries({ queryKey: ['jobs'] })
      // Fire-and-forget: re-scrape in background so jobs refresh with new profile
      jobsApi.scan().catch(() => undefined)
    },
  })

  const applyResume = async (resumeId: string): Promise<ResumeProfileFields> => {
    setApplyingResume(true)
    try {
      const res = await resumeApi.getProfileFields(resumeId)
      return res.data.data ?? {}
    } finally {
      setApplyingResume(false)
    }
  }

  return {
    profile,
    resumes,
    pendingSkills: profile?.skills.filter((s) => s.inferred && !s.confirmed) ?? [],
    confirmedSkills: profile?.skills.filter((s) => s.confirmed) ?? [],
    isLoading,
    isConfirming: confirmMutation.isPending,
    isSaving: updateMutation.isPending,
    isApplyingResume: applyingResume,
    confirmSkills: confirmMutation.mutate,
    updatePreferences: updateMutation.mutate,
    applyResume,
  }
}
