import { useState } from 'react'
import { Wand2, FileText, Mail, Copy, Check, RotateCcw, Loader2 } from 'lucide-react'
import { clsx } from 'clsx'
import { useGeneratorViewModel } from '@/viewmodels/useGeneratorViewModel'

export default function Generator() {
  const {
    jobs, jobsLoading, selectedJob,
    result, isGenerating, error,
    selectJob, generateResume, generateCoverLetter, clearResult,
  } = useGeneratorViewModel()

  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    if (!result?.text) return
    await navigator.clipboard.writeText(result.text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">AI Generator</h1>
        <p className="text-gray-500 mt-1">Generate a tailored resume or cover letter for any job using local AI.</p>
      </div>

      {/* ── Job picker ── */}
      <div className="card space-y-3">
        <label className="label">Select a job to generate for</label>
        {jobsLoading ? (
          <p className="text-sm text-gray-400">Loading jobs…</p>
        ) : (
          <select
            className="input"
            value={selectedJob?.id ?? ''}
            onChange={(e) => selectJob(e.target.value)}
          >
            <option value="">— Pick a job —</option>
            {jobs.map((job) => (
              <option key={job.id} value={job.id}>
                {job.title} · {job.company}
              </option>
            ))}
          </select>
        )}

        {selectedJob && (
          <div className="flex flex-wrap gap-2 mt-1">
            {selectedJob.requiredSkills.slice(0, 8).map((s) => (
              <span key={s} className="px-2 py-0.5 bg-gray-100 text-gray-600 rounded text-xs">{s}</span>
            ))}
          </div>
        )}
      </div>

      {/* ── Action buttons ── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <button
          onClick={generateResume}
          disabled={!selectedJob || isGenerating}
          className={clsx(
            'card flex items-center gap-4 text-left transition-shadow hover:shadow-md',
            'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:shadow-none',
          )}
        >
          <div className="p-3 bg-brand-50 rounded-lg shrink-0">
            <FileText size={22} className="text-brand-600" />
          </div>
          <div>
            <p className="font-semibold text-gray-900">Optimize Resume</p>
            <p className="text-sm text-gray-500 mt-0.5">Rewrite your resume to target this job's keywords (ATS-ready)</p>
          </div>
        </button>

        <button
          onClick={generateCoverLetter}
          disabled={!selectedJob || isGenerating}
          className={clsx(
            'card flex items-center gap-4 text-left transition-shadow hover:shadow-md',
            'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:shadow-none',
          )}
        >
          <div className="p-3 bg-purple-50 rounded-lg shrink-0">
            <Mail size={22} className="text-purple-600" />
          </div>
          <div>
            <p className="font-semibold text-gray-900">Write Cover Letter</p>
            <p className="text-sm text-gray-500 mt-0.5">Generate a 3-paragraph cover letter tailored to the role</p>
          </div>
        </button>
      </div>

      {/* ── Generating state ── */}
      {isGenerating && (
        <div className="card flex items-center gap-4 border-brand-200 bg-brand-50">
          <Loader2 size={20} className="text-brand-600 animate-spin shrink-0" />
          <div>
            <p className="font-medium text-brand-800">AI is writing…</p>
            <p className="text-sm text-brand-600 mt-0.5">
              Ollama is processing your request locally. This takes 20–60 seconds.
            </p>
          </div>
        </div>
      )}

      {/* ── Error ── */}
      {error && (
        <div className="card border-l-4 border-red-400 bg-red-50 text-red-800 text-sm">
          {error}
        </div>
      )}

      {/* ── Result ── */}
      {result && (
        <div className="card space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Wand2 size={16} className="text-brand-600" />
              <span className="font-semibold text-gray-900">
                {result.mode === 'resume' ? 'Optimized Resume' : 'Cover Letter'}
              </span>
              <span className="text-xs text-gray-400">for {selectedJob?.title} · {selectedJob?.company}</span>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={handleCopy}
                className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-brand-600 transition-colors"
              >
                {copied ? <Check size={15} className="text-green-600" /> : <Copy size={15} />}
                {copied ? 'Copied!' : 'Copy'}
              </button>
              <button
                onClick={clearResult}
                className="flex items-center gap-1.5 text-sm text-gray-400 hover:text-gray-600 transition-colors"
              >
                <RotateCcw size={14} /> Clear
              </button>
            </div>
          </div>

          <pre className="whitespace-pre-wrap text-sm text-gray-800 bg-gray-50 rounded-lg p-4 border border-gray-200 max-h-[500px] overflow-y-auto leading-relaxed font-sans">
            {result.text}
          </pre>
        </div>
      )}
    </div>
  )
}
