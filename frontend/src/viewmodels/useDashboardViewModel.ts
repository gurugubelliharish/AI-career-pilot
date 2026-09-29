import { useQuery } from '@tanstack/react-query'
import { trackerApi } from '@/api/endpoints'
import type { DashboardStats } from '@/types'

export interface DashboardViewModel {
  stats: DashboardStats | undefined
  isLoading: boolean
}

export function useDashboardViewModel(): DashboardViewModel {
  const { data: stats, isLoading } = useQuery({
    queryKey: ['tracker-stats'],
    queryFn: () => trackerApi.stats().then((r) => r.data.data),
  })

  return { stats, isLoading }
}
