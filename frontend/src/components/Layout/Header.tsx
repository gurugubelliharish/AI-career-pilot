import { Bell, LogOut } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { useNavigate } from 'react-router-dom'

export default function Header() {
  const { name, logout } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="h-14 bg-white border-b border-gray-200 flex items-center justify-between px-6">
      <div />
      <div className="flex items-center gap-4">
        <button className="relative p-1.5 text-gray-500 hover:text-gray-700 rounded-lg hover:bg-gray-100">
          <Bell size={18} />
        </button>

        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-brand-600 rounded-full flex items-center justify-center text-white text-sm font-semibold">
            {name?.charAt(0).toUpperCase() ?? 'U'}
          </div>
          <span className="text-sm font-medium text-gray-700">{name}</span>
        </div>

        <button
          onClick={handleLogout}
          className="p-1.5 text-gray-500 hover:text-red-600 rounded-lg hover:bg-red-50 transition-colors"
          title="Logout"
        >
          <LogOut size={18} />
        </button>
      </div>
    </header>
  )
}
