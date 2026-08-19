import { useState, useRef, useEffect, useCallback } from 'react'
import './App.css'

const API_BASE = import.meta.env.VITE_API_BASE || 'https://docmind-81ju.onrender.com/api'
const STORAGE_KEY = 'docmind_session'
const QUESTION_MAX_LENGTH = 300
const STATUS_POLL_INTERVAL = 15000

const PIPELINE_STEPS = ['EXTRACT', 'CHUNK', 'EMBED', 'STORE']

function friendlyError(rawMessage) {
  if (!rawMessage) return 'Something went wrong. Please try again.'
  if (rawMessage.includes('503') || rawMessage.toLowerCase().includes('service unavailable')) {
    return 'The AI service is briefly busy. Please wait a few seconds and try again.'
  }
  if (rawMessage.includes('429') || rawMessage.toLowerCase().includes('too many requests')) {
    return "You've hit the free-tier rate limit. Please wait a minute before trying again."
  }
  if (rawMessage.toLowerCase().includes('could not reach backend')) {
    return 'Could not reach the backend. Make sure it\'s running on port 8080.'
  }
  return rawMessage
}

function formatFileSize(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

function loadSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function saveSession(session) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  } catch {
    // storage full or unavailable — fail silently, not critical
  }
}

export default function App() {
  const saved = loadSession()

  const [fileName, setFileName] = useState(saved?.fileName || null)
  const [fileSize, setFileSize] = useState(saved?.fileSize || null)
  const [uploading, setUploading] = useState(false)
  const [dragActive, setDragActive] = useState(false)
  const [activeStep, setActiveStep] = useState(saved?.fileName ? PIPELINE_STEPS.length : -1)
  const [uploadResult, setUploadResult] = useState(saved?.uploadResult || null)
  const [uploadError, setUploadError] = useState(null)

  const [question, setQuestion] = useState(saved?.question || '')
  const [asking, setAsking] = useState(false)
  const [answer, setAnswer] = useState(saved?.answer || null)
  const [askError, setAskError] = useState(null)

  const [restoredNotice, setRestoredNotice] = useState(!!saved?.uploadResult)
  const [backendStatus, setBackendStatus] = useState('checking') // 'checking' | 'online' | 'offline'

  const fileInputRef = useRef(null)

  // Persist to localStorage whenever meaningful state changes.
  // Note: this only restores what the SCREEN shows after a refresh — if the
  // backend itself restarted, its in-memory vector store is empty, so you'll
  // still need to re-upload the PDF before asking a new question.
  useEffect(() => {
    saveSession({ fileName, fileSize, uploadResult, question, answer })
  }, [fileName, fileSize, uploadResult, question, answer])

  // Poll the backend's real health/status instead of showing a hardcoded badge.
  const checkBackendStatus = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/documents/status`)
      setBackendStatus(res.ok ? 'online' : 'offline')
    } catch {
      setBackendStatus('offline')
    }
  }, [])

  useEffect(() => {
    checkBackendStatus()
    const interval = setInterval(checkBackendStatus, STATUS_POLL_INTERVAL)
    return () => clearInterval(interval)
  }, [checkBackendStatus])

  async function handleUpload(selectedFile) {
    if (!selectedFile || uploading) return
    if (selectedFile.type !== 'application/pdf') {
      setUploadError('Only PDF files are supported.')
      return
    }

    setFileName(selectedFile.name)
    setFileSize(selectedFile.size)
    setUploading(true)
    setUploadError(null)
    setUploadResult(null)
    setActiveStep(0)
    setRestoredNotice(false)
    // Clear any previous Q&A state since the document is changing
    setAnswer(null)
    setAskError(null)
    setQuestion('')

    const stepTimer = setInterval(() => {
      setActiveStep((prev) => (prev < PIPELINE_STEPS.length - 1 ? prev + 1 : prev))
    }, 500)

    try {
      const formData = new FormData()
      formData.append('file', selectedFile)

      const res = await fetch(`${API_BASE}/documents/upload`, {
        method: 'POST',
        body: formData,
      })
      const data = await res.json()

      clearInterval(stepTimer)
      setActiveStep(PIPELINE_STEPS.length)

      if (!res.ok) {
        setUploadError(friendlyError(data.error))
      } else {
        setUploadResult(data)
      }
      checkBackendStatus()
    } catch (err) {
      clearInterval(stepTimer)
      setUploadError('Could not reach the backend. Is it running on :8080?')
      setBackendStatus('offline')
    } finally {
      setUploading(false)
    }
  }

  function handleDragOver(e) {
    e.preventDefault()
    if (!uploading) setDragActive(true)
  }

  function handleDragLeave(e) {
    e.preventDefault()
    setDragActive(false)
  }

  function handleDrop(e) {
    e.preventDefault()
    setDragActive(false)
    if (uploading) return
    const droppedFile = e.dataTransfer.files?.[0]
    if (droppedFile) handleUpload(droppedFile)
  }

  async function handleClear() {
    try {
      await fetch(`${API_BASE}/documents/clear`, { method: 'DELETE' })
    } catch (err) {
      // Even if this fails, reset the UI so the user can start over
    }
    setFileName(null)
    setFileSize(null)
    setUploadResult(null)
    setUploadError(null)
    setActiveStep(-1)
    setAnswer(null)
    setAskError(null)
    setQuestion('')
    setRestoredNotice(false)
    localStorage.removeItem(STORAGE_KEY)
    if (fileInputRef.current) fileInputRef.current.value = ''
    checkBackendStatus()
  }

  async function handleAsk(e) {
    e.preventDefault()
    if (!question.trim() || asking) return

    setAsking(true)
    setAskError(null)
    setAnswer(null)
    setRestoredNotice(false)

    try {
      const res = await fetch(`${API_BASE}/query`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question: question.trim() }),
      })
      const data = await res.json()

      if (!res.ok) {
        setAskError(friendlyError(data.error))
      } else {
        setAnswer(data.answer)
      }
      checkBackendStatus()
    } catch (err) {
      setAskError('Could not reach the backend. Is it running on :8080?')
      setBackendStatus('offline')
    } finally {
      setAsking(false)
    }
  }

  const statusLabel =
    backendStatus === 'online' ? '200' : backendStatus === 'offline' ? 'OFFLINE' : '...'

  return (
    <div className="app">
      <header className="topbar">
        <div className="wordmark display">
          DOC<span className="accent">MIND</span>
        </div>
        <div className={`endpoint-badge mono ${backendStatus}`}>
          POST /api/query <span className={`status-dot ${backendStatus}`} /> {statusLabel}
        </div>
      </header>

      <main className="main">
        <section className="panel upload-panel">
          <div className="panel-header-row">
            <div>
              <h2 className="display panel-title">01 — Ingest</h2>
              <p className="panel-sub">Upload a PDF. It gets extracted, chunked, embedded, and stored as vectors.</p>
            </div>
            {(fileName || uploadResult) && (
              <button type="button" className="clear-button mono" onClick={handleClear}>
                CLEAR
              </button>
            )}
          </div>

          {restoredNotice && (
            <div className="notice-box mono">
              ↻ Restored your last session's screen. If the backend restarted since then,
              re-upload the PDF before asking a new question.
            </div>
          )}

          <div
            className={`dropzone ${fileName ? 'has-file' : ''} ${uploading ? 'disabled' : ''} ${dragActive ? 'drag-active' : ''}`}
            onClick={() => !uploading && fileInputRef.current?.click()}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept="application/pdf"
              hidden
              onChange={(e) => handleUpload(e.target.files[0])}
              disabled={uploading}
            />
            <span className="mono">
              {fileName || (dragActive ? 'DROP TO UPLOAD' : 'CLICK OR DRAG A .PDF HERE')}
            </span>
            {fileSize != null && <span className="file-size mono">{formatFileSize(fileSize)}</span>}
          </div>

          <div className="pipeline">
            {PIPELINE_STEPS.map((step, i) => (
              <div key={step} className={`pipeline-step mono ${i <= activeStep ? 'active' : ''}`}>
                {i < activeStep || (i === activeStep && !uploading) ? (
                  <span className="pipeline-dot done" />
                ) : i === activeStep && uploading ? (
                  <span className="pipeline-dot spinning" />
                ) : (
                  <span className="pipeline-dot" />
                )}
                {step}
              </div>
            ))}
          </div>

          {uploadResult && (
            <div className="result-box mono">
              ✓ {uploadResult.chunksCreated} chunks created · {uploadResult.totalChunksInStore} total in store
            </div>
          )}
          {uploadError && <div className="result-box error mono">✗ {uploadError}</div>}
        </section>

        <section className="panel ask-panel">
          <h2 className="display panel-title">02 — Retrieve &amp; Answer</h2>
          <p className="panel-sub">Ask a question. It's answered using only your uploaded document.</p>

          <form onSubmit={handleAsk} className="ask-form">
            <div className="ask-input-wrap">
              <input
                type="text"
                value={question}
                onChange={(e) => setQuestion(e.target.value.slice(0, QUESTION_MAX_LENGTH))}
                placeholder="e.g. What does this document say about..."
                className="ask-input"
                maxLength={QUESTION_MAX_LENGTH}
                disabled={asking}
              />
              <span className="char-counter mono">
                {question.length}/{QUESTION_MAX_LENGTH}
              </span>
            </div>
            <button type="submit" className="ask-button mono" disabled={asking || !question.trim()}>
              {asking ? (
                <>
                  <span className="spinner" /> THINKING…
                </>
              ) : (
                'ASK →'
              )}
            </button>
          </form>

          {askError && <div className="result-box error mono">✗ {askError}</div>}

          {answer && (
            <div className="answer-box">
              <div className="answer-label mono">ANSWER</div>
              <p className="answer-text">{answer}</p>
            </div>
          )}
        </section>
      </main>

      <footer className="footer mono">
        DocMind — RAG document assistant · Spring Boot + Gemini (embeddings) + Groq (generation) + React
      </footer>
    </div>
  )
}