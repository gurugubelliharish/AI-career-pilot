import { useState, useCallback } from 'react'
import { useQuery } from '@tanstack/react-query'
import { jobsApi, generatorApi } from '@/api/endpoints'
import type { Job } from '@/types'

export type GeneratorMode = 'resume' | 'coverLetter'

export interface GeneratorResult {
  mode: GeneratorMode
  text: string
}

export interface GeneratorViewModel {
  jobs: Job[]
  jobsLoading: boolean
  selectedJob: Job | null
  result: GeneratorResult | null
  isGenerating: boolean
  error: string
  selectJob: (jobId: string) => void
  generateResume: () => Promise<void>
  generateCoverLetter: () => Promise<void>
  clearResult: () => void
}

export function useGeneratorViewModel(): GeneratorViewModel {
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null)
  const [result, setResult] = useState<GeneratorResult | null>(null)
  const [isGenerating, setIsGenerating] = useState(false)
  const [error, setError] = useState('')

  const { data, isLoading: jobsLoading } = useQuery({
    queryKey: ['jobs'],
    queryFn: () => jobsApi.list(0, 50).then((r) => r.data.data),
  })

  const jobs: Job[] = data?.content ?? []
  const selectedJob = jobs.find((j) => j.id === selectedJobId) ?? null

  const selectJob = useCallback((jobId: string) => {
    setSelectedJobId(jobId)
    setResult(null)
    setError('')
  }, [])

  const generate = useCallback(async (mode: GeneratorMode) => {
    if (!selectedJobId) return
    setIsGenerating(true)
    setError('')
    setResult(null)
    try {
      if (mode === 'resume') {
        const res = await generatorApi.generateResume(selectedJobId)
        setResult({ mode, text: res.data.data.optimizedResume })
      } else {
        const res = await generatorApi.generateCoverLetter(selectedJobId)
        setResult({ mode, text: res.data.data.coverLetter })
      }
    } catch {
      setError('Generation failed. Make sure Ollama is running and your resume is uploaded.')
    } finally {
      setIsGenerating(false)
    }
  }, [selectedJobId])

  return {
    jobs,
    jobsLoading,
    selectedJob,
    result,
    isGenerating,
    error,
    selectJob,
    generateResume: () => generate('resume'),
    generateCoverLetter: () => generate('coverLetter'),
    clearResult: () => { setResult(null); setError('') },
  }
}
