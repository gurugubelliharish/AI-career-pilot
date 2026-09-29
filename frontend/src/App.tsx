import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import Layout from '@/components/Layout/Layout'
import Dashboard from '@/pages/Dashboard'
import Jobs from '@/pages/Jobs'
import Matches from '@/pages/Matches'
import Generator from '@/pages/Generator'
import Resume from '@/pages/Resume'
import Profile from '@/pages/Profile'
import Tracker from '@/pages/Tracker'
import Login from '@/pages/Auth/Login'
import Register from '@/pages/Auth/Register'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Dashboard />} />
          <Route path="jobs" element={<Jobs />} />
          <Route path="matches" element={<Matches />} />
          <Route path="generator" element={<Generator />} />
          <Route path="resume" element={<Resume />} />
          <Route path="profile" element={<Profile />} />
          <Route path="tracker" element={<Tracker />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
