import { useEffect, useState } from 'react'
import './App.css'

export default function App() {
  const [status, setStatus] = useState('loading...')

  useEffect(() => {
    fetch('/api/health')
      .then(r => r.text())
      .then(setStatus)
      .catch(() => setStatus('error'))
  }, [])

  return (
    <div style={{ padding: 24, fontFamily: 'system-ui, sans-serif' }}>
      <h1>Scout</h1>
      <p>Backend health: <b>{status}</b></p>
    </div>
  )
}
