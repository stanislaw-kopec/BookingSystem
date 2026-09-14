import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/layout/AppLayout'
import { AuthProvider } from './features/auth/AuthProvider'
import { RequireAdmin } from './features/auth/components/RequireAdmin'
import { RequireAuthenticated } from './features/auth/components/RequireAuthenticated'
import { RequireClient } from './features/auth/components/RequireClient'
import { RequireStaff } from './features/auth/components/RequireStaff'
import { AdminScheduleSettingsPage } from './pages/AdminScheduleSettingsPage'
import { AccountSecurityPage } from './pages/AccountSecurityPage'
import { AdminAccountsPage } from './pages/AdminAccountsPage'
import { AppointmentsPage } from './pages/AppointmentsPage'
import { HomePage } from './pages/HomePage'
import { MyAppointmentsPage } from './pages/MyAppointmentsPage'
import { ProfilePage } from './pages/ProfilePage'
import { StaffAppointmentDetailsPage } from './pages/StaffAppointmentDetailsPage'
import { StaffAppointmentsPage } from './pages/StaffAppointmentsPage'
import { StaffSchedulePage } from './pages/StaffSchedulePage'
import { VehiclePage } from './pages/VehiclePage'
import { VehiclesPage } from './pages/VehiclesPage'
import './App.css'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<HomePage />} />
            <Route path="appointments" element={<AppointmentsPage />} />
            <Route path="my-appointments" element={<RequireClient><MyAppointmentsPage /></RequireClient>} />
            <Route path="profile" element={<RequireClient><ProfilePage /></RequireClient>} />
            <Route path="account/security" element={<RequireAuthenticated><AccountSecurityPage /></RequireAuthenticated>} />
            <Route path="vehicles" element={<RequireClient><VehiclesPage /></RequireClient>} />
            <Route path="vehicles/:vehicleId" element={<RequireClient><VehiclePage /></RequireClient>} />
            <Route path="staff/schedule" element={<RequireStaff><StaffSchedulePage /></RequireStaff>} />
            <Route path="staff/appointments" element={<RequireStaff><StaffAppointmentsPage /></RequireStaff>} />
            <Route path="staff/appointments/:appointmentId" element={<RequireStaff><StaffAppointmentDetailsPage /></RequireStaff>} />
            <Route path="admin/schedule-settings" element={<RequireAdmin><AdminScheduleSettingsPage /></RequireAdmin>} />
            <Route path="admin/accounts" element={<RequireAdmin><AdminAccountsPage /></RequireAdmin>} />
            <Route path="admin/staff-accounts" element={<Navigate to="/admin/accounts" replace />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
