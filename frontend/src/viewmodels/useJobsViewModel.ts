import { useState, useEffect, useRef, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { jobsApi, trackerApi } from '@/api/endpoints'
import type { Job } from '@/types'

export interface JobsViewModel {
  jobs: Job[]
  totalElements: number
  isLoading: boolean
  isSaving: boolean
  scanningSource: string | null  // which source is currently scanning, null = none
  savedJobIds: Set<string>
  saveJob: (jobId: string) => void
  refresh: (source?: string, keywords?: string[]) => void
}

const POLL_INTERVAL_MS = 3_000
const SCAN_DURATION_MS = 60_000

export function useJobsViewModel(): JobsViewModel {
  const qc = useQueryClient()
  const [scanningSource, setScanningSource] = useState<string | null>(null)
  const scanTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['jobs'],
    queryFn: () => jobsApi.list().then((r) => r.data.data),
    staleTime: 0,
    refetchInterval: scanningSource !== null ? POLL_INTERVAL_MS : false,
  })

  const { data: applications = [] } = useQuery({
    queryKey: ['applications'],
    queryFn: () => trackerApi.list().then((r) => r.data.data ?? []),
  })

  const saveMutation = useMutation({
    mutationFn: (jobId: string) => trackerApi.saveJob(jobId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['applications'] }),
  })

  const refresh = useCallback((source?: string, keywords?: string[]) => {
    const key = source ?? '__all__'
    if (scanningSource === key) return

    setScanningSource(key)
    jobsApi.scan(source, keywords).catch(() => undefined)

    if (scanTimerRef.current) clearTimeout(scanTimerRef.current)
    scanTimerRef.current = setTimeout(() => {
      setScanningSource(null)
      refetch()
    }, SCAN_DURATION_MS)
  }, [scanningSource, refetch])

  useEffect(() => () => {
    if (scanTimerRef.current) clearTimeout(scanTimerRef.current)
  }, [])

  const savedJobIds = new Set(
    (applications as { job: { id: string } }[]).map((a) => a.job.id)
  )

  return {
    jobs: data?.content ?? [],
    totalElements: data?.totalElements ?? 0,
    isLoading,
    isSaving: saveMutation.isPending,
    scanningSource,
    savedJobIds,
    saveJob: saveMutation.mutate,
    refresh,
  }
}
