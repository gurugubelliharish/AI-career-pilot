import { clsx } from 'clsx'
import Badge from '@/components/common/Badge'
import { useTrackerViewModel } from '@/viewmodels/useTrackerViewModel'
import type { ApplicationStatus } from '@/types'

const STATUS_OPTIONS: ApplicationStatus[] = ['SAVED', 'APPLIED', 'INTERVIEW', 'REJECTED', 'OFFER']

const statusBadgeVariant = (s: ApplicationStatus) => {
  const map: Record<ApplicationStatus, 'saved' | 'applied' | 'interview' | 'rejected' | 'offer'> = {
    SAVED: 'saved', APPLIED: 'applied', INTERVIEW: 'interview', REJECTED: 'rejected', OFFER: 'offer',
  }
  return map[s]
}

export default function Tracker() {
  const { applications, isLoading, updateStatus } = useTrackerViewModel()

  if (isLoading) {
    return <div className="flex items-center justify-center h-64 text-gray-400">Loading…</div>
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Application Tracker</h1>
        <p className="text-gray-500 mt-1">{applications.length} applications tracked</p>
      </div>

      <div className="space-y-3">
        {applications.map((app) => (
          <div key={app.id} className="card">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="font-semibold text-gray-900">{app.job.title}</h3>
                <p className="text-sm text-gray-600">{app.job.company}</p>
                {app.appliedAt && (
                  <p className="text-xs text-gray-400 mt-1">
                    Applied {new Date(app.appliedAt).toLocaleDateString()}
                  </p>
                )}
              </div>

              <div className="flex items-center gap-3">
                <Badge label={app.status} variant={statusBadgeVariant(app.status)} />
                <select
                  value={app.status}
                  onChange={(e) => updateStatus(app.id, e.target.value)}
                  className={clsx(
                    'text-sm border border-gray-200 rounded-lg px-2 py-1',
                    'focus:outline-none focus:ring-2 focus:ring-brand-500',
                  )}
                >
                  {STATUS_OPTIONS.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>
        ))}

        {applications.length === 0 && (
          <div className="card text-center py-16 text-gray-400">
            <p>No applications yet. Browse jobs and start saving opportunities.</p>
          </div>
        )}
      </div>
    </div>
  )
}
