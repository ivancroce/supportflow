import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from '@/components/AppShell'
import { Login } from '@/routes/Login'
import { OAuthCallback } from '@/routes/OAuthCallback'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { RootRedirect } from '@/routes/RootRedirect'

// Temporary placeholder for views built in later commits (board, detail, settings).
function ComingSoon({ title }: { title: string }) {
  return (
    <div className="flex h-full items-center justify-center p-10 text-sm text-ink-3">
      {title} — coming next.
    </div>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/oauth/callback" element={<OAuthCallback />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<RootRedirect />} />
        <Route path="/projects/new" element={<ComingSoon title="New project" />} />
        <Route path="/projects/:projectId" element={<AppShell />}>
          <Route path="tickets" element={<ComingSoon title="Ticket board" />} />
          <Route path="tickets/:ticketId" element={<ComingSoon title="Ticket detail" />} />
          <Route path="settings" element={<ComingSoon title="Project settings" />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
