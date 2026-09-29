import { NavLink } from 'react-router-dom'
import { LayoutDashboard, Briefcase, Zap, Wand2, FileText, User, CheckSquare, Zap as Logo } from 'lucide-react'
import { clsx } from 'clsx'

const navItems = [
  { to: '/',          icon: LayoutDashboard, label: 'Dashboard'  },
  { to: '/jobs',      icon: Briefcase,       label: 'Jobs'        },
  { to: '/matches',   icon: Zap,             label: 'AI Matches'  },
  { to: '/generator', icon: Wand2,           label: 'Generator'   },
  { to: '/resume',    icon: FileText,        label: 'Resume'      },
  { to: '/profile',   icon: User,            label: 'Profile'     },
  { to: '/tracker',   icon: CheckSquare,     label: 'Tracker'     },
]

export default function Sidebar() {
  return (
    <aside className="w-60 bg-white border-r border-gray-200 flex flex-col">
      <div className="flex items-center gap-2 px-6 py-5 border-b border-gray-200">
        <Logo className="text-brand-600" size={22} />
        <span className="font-bold text-gray-900 text-lg">AI Job Radar</span>
      </div>

      <nav className="flex-1 px-3 py-4 space-y-1">
        {navItems.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              clsx(
                'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                isActive
                  ? 'bg-brand-50 text-brand-700'
                  : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900',
              )
            }
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
