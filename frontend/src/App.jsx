import { useEffect, useState } from 'react'
import Sidebar from './Sidebar.jsx'

export default function App() {
  const [currentUser, setCurrentUser] = useState(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let cancelled = false
    fetch('/api/me', { headers: { Accept: 'application/json' } })
      .then((response) => {
        if (response.status === 401) {
          window.location.assign('/login')
          return null
        }
        if (!response.ok) {
          throw new Error(`/api/me answered ${response.status}`)
        }
        return response.json()
      })
      .then((body) => { if (!cancelled && body) setCurrentUser(body) })
      .catch(() => { if (!cancelled) setFailed(true) })
    return () => { cancelled = true }
  }, [])

  return (
    <div className="shell">
      <header className="topbar">
        <span className="title">Social Media Harvester</span>
        {currentUser && <span className="who">{currentUser.email}</span>}
        {failed && <span role="alert" className="error">Could not load your account. Try reloading the page.</span>}
      </header>
      <Sidebar />
      <main className="content" />
    </div>
  )
}
