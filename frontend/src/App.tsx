import { AuthProvider } from './features/auth/AuthProvider'
import { HomePage } from './pages/HomePage'
import './App.css'

export default function App() {
  return (
    <AuthProvider>
      <HomePage />
    </AuthProvider>
  )
}
