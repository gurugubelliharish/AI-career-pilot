import { Zap, TrendingUp, Clock, Target, CheckCircle, XCircle } from 'lucide-react'
import { clsx } from 'clsx'
import { useMatchesViewModel } from '@/viewmodels/useMatchesViewModel'
import type { MatchResult } from '@/types'

const PRIORITY_STYLES: Record<string, string> = {
  CRITICAL: 'bg-red-100 text-red-800 border-red-200',
  HIGH:     'bg-orange-100 text-orange-800 border-orange-200',
  MEDIUM:   'bg-yellow-100 text-yellow-800 border-yellow-200',
  LOW:      'bg-gray-100 text-gray-700 border-gray-200',
}


function ScoreBar({ value, color }: { value: number; color: string }) {
  return (
    <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden w-24">
      <div className={clsx('h-full rounded-full', color)} style={{ width: `${value}%` }} />
    </div>
  )
}

function MatchCard({ match }: { match: MatchResult }) {
  return (
    <div className="card hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-1">
            <h3 className="font-semibold text-gray-900">{match.jobTitle}</h3>
            <span className={clsx('px-2 py-0.5 rounded text-xs font-semibold border', PRIORITY_STYLES[match.priority])}>
              {match.priority}
            </span>
          </div>
          <p className="text-sm text-gray-600">{match.company}</p>
          <p className="text-xs text-gray-400 mt-0.5">{match.postedAgo}</p>

          {/* Score row */}
          <div className="flex items-center gap-5 mt-3 flex-wrap">
            <div className="flex items-center gap-2">
              <Zap size={13} className="text-brand-600" />
              <span className="text-xs text-gray-500">Match</span>
              <ScoreBar value={match.matchScore} color="bg-brand-500" />
              <span className="text-xs font-semibold text-gray-700">{match.matchScore}%</span>
            </div>
            <div className="flex items-center gap-2">
              <Clock size={13} className="text-green-600" />
              <span className="text-xs text-gray-500">Fresh</span>
              <ScoreBar value={match.freshnessScore} color="bg-green-500" />
              <span className="text-xs font-semibold text-gray-700">{match.freshnessScore}%</span>
            </div>
            <div className="flex items-center gap-2">
              <Target size={13} className="text-purple-600" />
              <span className="text-xs text-gray-500">Goal</span>
              <ScoreBar value={match.goalAlignmentScore} color="bg-purple-400" />
              <span className="text-xs font-semibold text-gray-700">{match.goalAlignmentScore}%</span>
            </div>
          </div>

          {/* Skills */}
          <div className="flex flex-wrap gap-3 mt-3">
            {match.matchedSkills.slice(0, 5).map((s) => (
              <span key={s} className="flex items-center gap-1 text-xs text-green-700 bg-green-50 px-2 py-0.5 rounded border border-green-200">
                <CheckCircle size={11} /> {s}
              </span>
            ))}
            {match.missingSkills.slice(0, 3).map((s) => (
              <span key={s} className="flex items-center gap-1 text-xs text-red-600 bg-red-50 px-2 py-0.5 rounded border border-red-200">
                <XCircle size={11} /> {s}
              </span>
            ))}
            {match.missingSkills.length > 3 && (
              <span className="text-xs text-gray-400">+{match.missingSkills.length - 3} missing</span>
            )}
          </div>
        </div>

        <div className="shrink-0 text-right">
          <p className="text-2xl font-bold text-gray-900">{match.priorityScore}%</p>
          <p className="text-xs text-gray-400">Priority score</p>
        </div>
      </div>
    </div>
  )
}

export default function Matches() {
  const { matches, isLoading, criticalCount, highCount } = useMatchesViewModel()

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-3 text-gray-400">
        <Zap size={28} className="animate-pulse text-brand-500" />
        <p>Computing AI match scores…</p>
        <p className="text-xs">This may take a moment while Ollama processes embeddings.</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">AI Matches</h1>
        <p className="text-gray-500 mt-1">
          {matches.length} jobs ranked by priority ·{' '}
          <span className="text-red-600 font-medium">{criticalCount} critical</span>
          {' · '}
          <span className="text-orange-600 font-medium">{highCount} high</span>
        </p>
      </div>

      {matches.length === 0 && (
        <div className="card text-center py-16 text-gray-400">
          <TrendingUp size={32} className="mx-auto mb-3 opacity-40" />
          <p className="font-medium">No matches yet</p>
          <p className="text-sm mt-1">Upload your resume and set preferred roles to start matching.</p>
        </div>
      )}

      <div className="space-y-3">
        {matches.map((match) => (
          <MatchCard key={match.jobId} match={match} />
        ))}
      </div>
    </div>
  )
}
