import { useCallback, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { resumeApi } from '@/api/endpoints'
import type { Resume } from '@/types'

export type UploadState = 'idle' | 'uploading' | 'extracting' | 'done' | 'error'

export interface ResumeViewModel {
  resumes: Resume[]
  uploadState: UploadState
  message: string
  isProcessing: boolean
  onFileDrop: (files: File[]) => Promise<void>
  reset: () => void
  openResume: (id: string, fileName: string) => Promise<void>
}

export function useResumeViewModel(): ResumeViewModel {
  const qc = useQueryClient()
  const [uploadState, setUploadState] = useState<UploadState>('idle')
  const [message, setMessage] = useState('')

  const { data: resumeList = [] } = useQuery({
    queryKey: ['resumes'],
    queryFn: () => resumeApi.list().then((r) => r.data.data ?? []),
  })

  const onFileDrop = useCallback(async (files: File[]) => {
    const file = files[0]
    if (!file) return

    try {
      setUploadState('uploading')
      setMessage('Uploading resume…')
      const uploadRes = await resumeApi.upload(file)
      const resumeId = uploadRes.data?.data?.id

      setUploadState('extracting')
      setMessage('Extracting skills with AI…')
      await resumeApi.extractSkills(resumeId)

      setUploadState('done')
      setMessage('Skills extracted! Your resume is ready.')
      qc.invalidateQueries({ queryKey: ['resumes'] })
      qc.invalidateQueries({ queryKey: ['profile'] })
    } catch {
      setUploadState('error')
      setMessage('Upload failed. Please try again.')
    }
  }, [qc])

  const reset = useCallback(() => {
    setUploadState('idle')
    setMessage('')
  }, [])

  const openResume = useCallback(async (id: string, fileName: string) => {
    const res = await resumeApi.download(id)
    const blob = new Blob([res.data], { type: String(res.headers['content-type'] ?? 'application/pdf') })
    const url = URL.createObjectURL(blob)
    const win = window.open(url, '_blank')
    // Revoke after the new tab has loaded
    setTimeout(() => URL.revokeObjectURL(url), 10_000)
    if (!win) {
      // Fallback: trigger download if popup blocked
      const a = document.createElement('a')
      a.href = url
      a.download = fileName
      a.click()
    }
  }, [])

  return {
    resumes: resumeList,
    uploadState,
    message,
    isProcessing: uploadState === 'uploading' || uploadState === 'extracting',
    onFileDrop,
    reset,
    openResume,
  }
}
