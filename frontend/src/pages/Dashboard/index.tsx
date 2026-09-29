import { Briefcase, Send, MessageSquare, Trophy } from 'lucide-react'
import { useDashboardViewModel } from '@/viewmodels/useDashboardViewModel'

const STAT_CONFIG = [
  { key: 'saved'     as const, label: 'Jobs Saved',  Icon: Briefcase,     color: 'text-blue-600',   bg: 'bg-blue-50' },
  { key: 'applied'   as const, label: 'Applied',      Icon: Send,          color: 'text-orange-600', bg: 'bg-orange-50' },
  { key: 'interview' as const, label: 'Interviews',   Icon: MessageSquare, color: 'text-purple-600', bg: 'bg-purple-50' },
  { key: 'offer'     as const, label: 'Offers',       Icon: Trophy,        color: 'text-green-600',  bg: 'bg-green-50' },
]

export default function Dashboard() {
  const { stats } = useDashboardViewModel()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-500 mt-1">Your job search overview</p>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {STAT_CONFIG.map(({ key, label, Icon, color, bg }) => (
          <div key={key} className="card flex items-center gap-4">
            <div className={`p-3 rounded-lg ${bg}`}>
              <Icon size={20} className={color} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900">{stats?.[key] ?? 0}</p>
              <p className="text-sm text-gray-500">{label}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="card">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Recent Activity</h2>
        <p className="text-gray-500 text-sm">Your recent job applications will appear here.</p>
      </div>
    </div>
  )
}
