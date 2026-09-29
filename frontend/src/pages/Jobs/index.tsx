import { useState, useMemo, useEffect, useRef } from 'react'
import {
  MapPin, Clock, ExternalLink, BookmarkPlus, BookmarkCheck,
  RefreshCw, Search, SlidersHorizontal, X, Plus, Tag,
} from 'lucide-react'
import { useJobsViewModel } from '@/viewmodels/useJobsViewModel'
import type { Job } from '@/types'

// ── Source metadata ─────────────────────────────────────────────────────────
const SOURCE_LABELS: Record<string, string> = {
  REMOTEOK: 'RemoteOK',
  REMOTIVE: 'Remotive',
  WORKABLE: 'Workable',
  NAUKRI:   'Naukri',
}
const SOURCE_COLORS: Record<string, string> = {
  REMOTEOK: 'bg-sky-100 text-sky-700',
  REMOTIVE: 'bg-teal-100 text-teal-700',
  WORKABLE: 'bg-amber-100 text-amber-700',
  NAUKRI:   'bg-orange-100 text-orange-700',
}
const WORK_MODES  = ['REMOTE', 'HYBRID', 'ONSITE']
const WORK_LABELS: Record<string, string> = { REMOTE: 'Remote', HYBRID: 'Hybrid', ONSITE: 'On-site' }

const WORKABLE_KW_KEY = 'workable_keywords'

function sl(src: string) { return SOURCE_LABELS[src] ?? src }
function sc(src: string) { return SOURCE_COLORS[src] ?? 'bg-gray-100 text-gray-600' }
function toggleSet(arr: string[], v: string) {
  return arr.includes(v) ? arr.filter(x => x !== v) : [...arr, v]
}

interface Filters {
  keyword:   string
  locations: string[]
  workModes: string[]
  sortBy:    'newest' | 'oldest' | 'company'
}
const DEFAULT_FILTERS: Filters = { keyword: '', locations: [], workModes: [], sortBy: 'newest' }

function CheckPill({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`px-3 py-1.5 rounded-full text-xs font-medium border transition-colors ${
        active
          ? 'bg-brand-600 border-brand-600 text-white'
          : 'bg-white text-gray-600 border-gray-200 hover:border-brand-400 hover:text-brand-600'
      }`}
    >
      {label}
    </button>
  )
}

function applyFilters(jobs: Job[], f: Filters): Job[] {
  let r = jobs
  if (f.keyword.trim()) {
    const kw = f.keyword.toLowerCase()
    r = r.filter(j =>
      j.title.toLowerCase().includes(kw) ||
      j.company.toLowerCase().includes(kw) ||
      (j.location ?? '').toLowerCase().includes(kw) ||
      j.requiredSkills.some(s => s.toLowerCase().includes(kw))
    )
  }
  if (f.locations.length)
    r = r.filter(j => f.locations.some(l => (j.location ?? '').toLowerCase().includes(l.toLowerCase())))
  if (f.workModes.length)
    r = r.filter(j => j.workMode && f.workModes.includes(j.workMode.toUpperCase()))
  return [...r].sort((a, b) => {
    if (f.sortBy === 'newest') return new Date(b.postedAt ?? 0).getTime() - new Date(a.postedAt ?? 0).getTime()
    if (f.sortBy === 'oldest') return new Date(a.postedAt ?? 0).getTime() - new Date(b.postedAt ?? 0).getTime()
    return a.company.localeCompare(b.company)
  })
}

// ── Workable keyword chip editor ─────────────────────────────────────────────
function WorkableKeywordEditor({
  keywords, onChange,
}: { keywords: string[]; onChange: (kw: string[]) => void }) {
  const [input, setInput] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)

  const add = () => {
    const v = input.trim()
    if (v && !keywords.includes(v)) onChange([...keywords, v])
    setInput('')
    inputRef.current?.focus()
  }

  return (
    <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 space-y-3">
      <div className="flex items-center gap-2">
        <Tag size={14} className="text-amber-600" />
        <span className="text-sm font-medium text-amber-800">Workable search keywords</span>
        <span className="text-xs text-amber-600">— fetch all jobs matching any of these</span>
      </div>

      <div className="flex flex-wrap gap-2">
        {keywords.map(kw => (
          <span
            key={kw}
            className="inline-flex items-center gap-1 px-2.5 py-1 bg-amber-100 text-amber-800 rounded-full text-xs font-medium"
          >
            {kw}
            <button
              onClick={() => onChange(keywords.filter(k => k !== kw))}
              className="hover:text-red-500 transition-colors"
            >
              <X size={11} />
            </button>
          </span>
        ))}

        <div className="flex items-center gap-1">
          <input
            ref={inputRef}
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' || e.key === ',') { e.preventDefault(); add() } }}
            placeholder="Add keyword…"
            className="text-xs border border-amber-300 rounded-full px-2.5 py-1 bg-white focus:outline-none focus:border-amber-500 w-32"
          />
          <button
            onClick={add}
            disabled={!input.trim()}
            className="p-1 rounded-full bg-amber-200 hover:bg-amber-300 text-amber-800 disabled:opacity-40 transition-colors"
          >
            <Plus size={12} />
          </button>
        </div>
      </div>

      {keywords.length === 0 && (
        <p className="text-xs text-amber-600 italic">
          Add keywords like "java developer", "frontend", "python" to fetch targeted jobs.
        </p>
      )}
    </div>
  )
}

// ── Job card ─────────────────────────────────────────────────────────────────
function JobCard({ job, saved, onSave }: { job: Job; saved: boolean; onSave: () => void }) {
  return (
    <div className="card hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          {job.sourceUrl ? (
            <a
              href={job.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="font-semibold text-gray-900 hover:text-brand-600 hover:underline inline-flex items-center gap-1 group"
            >
              {job.title}
              <ExternalLink size={13} className="opacity-0 group-hover:opacity-60 transition-opacity shrink-0" />
            </a>
          ) : (
            <span className="font-semibold text-gray-900">{job.title}</span>
          )}

          <div className="flex items-center gap-2 flex-wrap mt-0.5">
            <span className="text-gray-600 text-sm">{job.company}</span>
            {job.workMode && (
              <span className="px-1.5 py-0.5 bg-blue-50 text-blue-600 rounded text-xs">
                {WORK_LABELS[job.workMode] ?? job.workMode}
              </span>
            )}
          </div>

          <div className="flex items-center gap-4 mt-2 text-sm text-gray-500">
            {job.location && <span className="flex items-center gap-1"><MapPin size={13} />{job.location}</span>}
            {job.postedAt && (
              <span className="flex items-center gap-1">
                <Clock size={13} />{new Date(job.postedAt).toLocaleDateString()}
              </span>
            )}
          </div>

          {job.requiredSkills.length > 0 && (
            <div className="flex flex-wrap gap-1.5 mt-3">
              {job.requiredSkills.slice(0, 6).map(s => (
                <span key={s} className="px-2 py-0.5 bg-gray-100 text-gray-600 rounded text-xs">{s}</span>
              ))}
              {job.requiredSkills.length > 6 && (
                <span className="text-gray-400 text-xs px-1">+{job.requiredSkills.length - 6} more</span>
              )}
            </div>
          )}
        </div>

        <button
          onClick={onSave}
          disabled={saved}
          className="p-2 text-gray-400 hover:text-brand-600 hover:bg-brand-50 rounded-lg transition-colors disabled:opacity-40 disabled:cursor-not-allowed shrink-0"
          title={saved ? 'Already saved' : 'Save job'}
        >
          {saved ? <BookmarkCheck size={18} className="text-brand-600" /> : <BookmarkPlus size={18} />}
        </button>
      </div>
    </div>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────
export default function Jobs() {
  const { jobs, totalElements, isLoading, scanningSource, savedJobIds, saveJob, refresh } = useJobsViewModel()
  const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS)
  const [showFilters, setShowFilters] = useState(true)
  const [selectedSource, setSelectedSource] = useState<string | null>(null)

  // Workable keywords — persisted in localStorage
  const [workableKeywords, setWorkableKeywords] = useState<string[]>(() => {
    try { return JSON.parse(localStorage.getItem(WORKABLE_KW_KEY) ?? '[]') } catch { return [] }
  })
  useEffect(() => {
    localStorage.setItem(WORKABLE_KW_KEY, JSON.stringify(workableKeywords))
  }, [workableKeywords])

  const sources = useMemo(() => Array.from(new Set(jobs.map((j: Job) => j.source))).sort(), [jobs])

  const locationOptions = useMemo(() => {
    const counts: Record<string, number> = {}
    jobs.forEach((j: Job) => {
      const loc = (j.location ?? '').split(',')[0].trim()
      if (loc && loc.toLowerCase() !== 'remote' && loc.length > 1)
        counts[loc] = (counts[loc] ?? 0) + 1
    })
    return Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 10).map(([l]) => l)
  }, [jobs])

  const visibleJobs = useMemo(() => {
    const src = selectedSource ? jobs.filter((j: Job) => j.source === selectedSource) : jobs
    return applyFilters(src, filters)
  }, [jobs, selectedSource, filters])

  const grouped = useMemo(() => {
    const srcOrder = selectedSource ? [selectedSource] : sources
    return srcOrder.reduce<Record<string, Job[]>>((acc, src) => {
      const g = visibleJobs.filter(j => j.source === src)
      if (g.length) acc[src] = g
      return acc
    }, {})
  }, [visibleJobs, sources, selectedSource])

  const hasActiveFilters = !!(filters.keyword || filters.locations.length || filters.workModes.length)

  // Is the current tab's source scanning?
  const currentScanKey = selectedSource ?? '__all__'
  const isCurrentScanning = scanningSource === currentScanKey

  const handleRefresh = () => {
    if (selectedSource === 'WORKABLE') {
      refresh('WORKABLE', workableKeywords)
    } else if (selectedSource) {
      refresh(selectedSource)
    } else {
      refresh()
    }
  }

  const refreshLabel = () => {
    if (isCurrentScanning) return selectedSource === 'WORKABLE' ? 'Fetching…' : 'Scanning…'
    if (selectedSource === 'WORKABLE') return workableKeywords.length ? 'Fetch Jobs' : 'Fetch All'
    if (selectedSource) return `Refresh ${sl(selectedSource)}`
    return 'Refresh All'
  }

  if (isLoading) {
    return <div className="flex items-center justify-center h-64 text-gray-400">Loading jobs…</div>
  }

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Job Discoveries</h1>
          <p className="text-gray-500 mt-0.5">
            {visibleJobs.length} of {totalElements} jobs
            {hasActiveFilters && <span className="text-brand-600 ml-1">· filtered</span>}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowFilters(f => !f)}
            className={`btn-secondary flex items-center gap-2 ${showFilters ? 'bg-gray-100' : ''}`}
          >
            <SlidersHorizontal size={15} /> Filters
            {hasActiveFilters && (
              <span className="ml-0.5 px-1.5 py-0.5 bg-brand-600 text-white rounded-full text-xs leading-none">
                {(filters.keyword ? 1 : 0) + filters.locations.length + filters.workModes.length}
              </span>
            )}
          </button>
          <button
            onClick={handleRefresh}
            disabled={isCurrentScanning}
            className="btn-secondary flex items-center gap-2 disabled:opacity-60"
          >
            <RefreshCw size={15} className={isCurrentScanning ? 'animate-spin' : ''} />
            {refreshLabel()}
          </button>
        </div>
      </div>

      {/* Filter Panel */}
      {showFilters && (
        <div className="card border border-gray-200 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-medium text-gray-900 text-sm">Filters</h3>
            {hasActiveFilters && (
              <button onClick={() => setFilters(DEFAULT_FILTERS)} className="text-xs text-brand-600 hover:underline flex items-center gap-1">
                <X size={11} /> Clear all
              </button>
            )}
          </div>

          <div className="relative">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
            <input
              type="text"
              placeholder="Search by title, company, skill, or location…"
              value={filters.keyword}
              onChange={e => setFilters(f => ({ ...f, keyword: e.target.value }))}
              className="input pl-9 text-sm"
            />
            {filters.keyword && (
              <button onClick={() => setFilters(f => ({ ...f, keyword: '' }))}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                <X size={14} />
              </button>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Work Type</p>
              <div className="flex flex-wrap gap-1.5">
                {WORK_MODES.map(m => (
                  <CheckPill key={m} label={WORK_LABELS[m]} active={filters.workModes.includes(m)}
                    onClick={() => setFilters(f => ({ ...f, workModes: toggleSet(f.workModes, m) }))} />
                ))}
              </div>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Location</p>
              <div className="flex flex-wrap gap-1.5">
                {locationOptions.slice(0, 6).map(loc => (
                  <CheckPill key={loc} label={loc} active={filters.locations.includes(loc)}
                    onClick={() => setFilters(f => ({ ...f, locations: toggleSet(f.locations, loc) }))} />
                ))}
              </div>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Sort By</p>
              <div className="flex flex-wrap gap-1.5">
                {(['newest', 'oldest', 'company'] as const).map(s => (
                  <CheckPill key={s}
                    label={s === 'newest' ? 'Newest' : s === 'oldest' ? 'Oldest' : 'Company A–Z'}
                    active={filters.sortBy === s}
                    onClick={() => setFilters(f => ({ ...f, sortBy: s }))} />
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Site tabs */}
      <div className="flex flex-wrap gap-2">
        <button
          onClick={() => setSelectedSource(null)}
          className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${
            selectedSource === null ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          All Sites ({totalElements})
        </button>
        {sources.map(src => (
          <button
            key={src}
            onClick={() => setSelectedSource(src === selectedSource ? null : src)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${
              selectedSource === src ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {sl(src)} ({jobs.filter((j: Job) => j.source === src).length})
            {scanningSource === src && (
              <RefreshCw size={11} className="inline ml-1.5 animate-spin opacity-60" />
            )}
          </button>
        ))}
      </div>

      {/* Workable keyword editor — shown only on Workable tab */}
      {selectedSource === 'WORKABLE' && (
        <WorkableKeywordEditor
          keywords={workableKeywords}
          onChange={setWorkableKeywords}
        />
      )}

      {/* Jobs grouped by source */}
      {Object.entries(grouped).map(([src, srcJobs]) => (
        <div key={src} className="space-y-3">
          <div className="flex items-center gap-3">
            <span className={`px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wide ${sc(src)}`}>
              {sl(src)}
            </span>
            <span className="text-sm text-gray-400">{srcJobs.length} jobs</span>
            <div className="flex-1 h-px bg-gray-100" />
          </div>
          <div className="space-y-3">
            {srcJobs.map((job: Job) => (
              <JobCard
                key={job.id}
                job={job}
                saved={savedJobIds.has(job.id)}
                onSave={() => saveJob(job.id)}
              />
            ))}
          </div>
        </div>
      ))}

      {visibleJobs.length === 0 && !isLoading && (
        <div className="card text-center py-16 text-gray-400">
          {hasActiveFilters
            ? <p>No jobs match your filters. <button onClick={() => setFilters(DEFAULT_FILTERS)} className="text-brand-600 underline">Clear filters</button></p>
            : selectedSource === 'WORKABLE'
              ? <p>No Workable jobs yet. Add keywords above and click <strong>Fetch Jobs</strong>.</p>
              : <p>No jobs discovered yet. Click <strong>Refresh</strong> to start scanning.</p>}
        </div>
      )}
    </div>
  )
}
