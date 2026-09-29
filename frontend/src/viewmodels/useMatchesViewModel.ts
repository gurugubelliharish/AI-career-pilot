import { useQuery } from '@tanstack/react-query'
import { matchesApi } from '@/api/endpoints'
import type { MatchResult } from '@/types'

export interface MatchesViewModel {
  matches: MatchResult[]
  isLoading: boolean
  criticalCount: number
  highCount: number
}

export function useMatchesViewModel(): MatchesViewModel {
  const { data: matches = [], isLoading } = useQuery({
    queryKey: ['matches'],
    queryFn: () => matchesApi.getMatches().then((r) => r.data.data ?? []),
    staleTime: 5 * 60 * 1000, // 5 min — avoid re-running embeddings on every render
  })

  return {
    matches: matches as MatchResult[],
    isLoading,
    criticalCount: (matches as MatchResult[]).filter((m) => m.priority === 'CRITICAL').length,
    highCount: (matches as MatchResult[]).filter((m) => m.priority === 'HIGH').length,
  }
}
