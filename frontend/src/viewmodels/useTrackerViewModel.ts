import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { trackerApi } from '@/api/endpoints'
import type { JobApplication } from '@/types'

export interface TrackerViewModel {
  applications: JobApplication[]
  isLoading: boolean
  isUpdating: boolean
  updateStatus: (id: string, status: string) => void
}

export function useTrackerViewModel(): TrackerViewModel {
  const qc = useQueryClient()

  const { data = [], isLoading } = useQuery({
    queryKey: ['applications'],
    queryFn: () => trackerApi.list().then((r) => r.data.data),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      trackerApi.updateStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['applications'] }),
  })

  return {
    applications: (data as JobApplication[]) ?? [],
    isLoading,
    isUpdating: updateMutation.isPending,
    updateStatus: (id, status) => updateMutation.mutate({ id, status }),
  }
}
