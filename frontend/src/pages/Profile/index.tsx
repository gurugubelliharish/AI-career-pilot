import { useState } from 'react'
import { User, MapPin, Briefcase, Target, Edit2, Save, X, FileText, CheckCircle, Loader2 } from 'lucide-react'
import Badge from '@/components/common/Badge'
import { TagInput } from '@/components/common/TagInput'
import { useProfileViewModel } from '@/viewmodels/useProfileViewModel'
import type { UpdatePreferencesRequest } from '@/types'

// workMode stored as comma-separated string in backend (e.g. "REMOTE,HYBRID")
const WORK_MODES = ['REMOTE', 'HYBRID', 'ONSITE'] as const
const WORK_MODE_LABELS: Record<string, string> = {
  REMOTE: 'Remote', HYBRID: 'Hybrid', ONSITE: 'On-site',
}

const EXP_LEVELS = ['FRESHER', '0_2', '2_5', '5_PLUS'] as const
const EXP_LABELS: Record<string, string> = {
  FRESHER: 'Fresher / Graduate', '0_2': '0–2 yrs', '2_5': '2–5 yrs', '5_PLUS': '5+ yrs',
}

function splitComma(value?: string): string[] {
  if (!value) return []
  return value.split(',').map((s) => s.trim()).filter(Boolean)
}

function joinComma(values: string[]): string {
  return values.join(',')
}

interface FormState extends Omit<UpdatePreferencesRequest, 'workMode' | 'experienceLevel'> {
  workModes: string[]
  experienceLevels: string[]
}

function TogglePill({
  label, active, onClick,
}: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`px-3 py-1.5 rounded-full text-sm font-medium border transition-colors ${
        active
          ? 'bg-brand-600 text-white border-brand-600'
          : 'bg-white text-gray-600 border-gray-300 hover:border-brand-400 hover:text-brand-600'
      }`}
    >
      {label}
    </button>
  )
}

function toggle(arr: string[], value: string): string[] {
  return arr.includes(value) ? arr.filter((v) => v !== value) : [...arr, value]
}

export default function Profile() {
  const {
    profile, resumes, pendingSkills, confirmedSkills,
    isLoading, isConfirming, isSaving, isApplyingResume,
    confirmSkills, updatePreferences, applyResume,
  } = useProfileViewModel()

  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState<FormState>({ workModes: [], experienceLevels: [] })
  const [resumePicker, setResumePicker] = useState(false)
  const [applyMsg, setApplyMsg] = useState('')

  const startEdit = () => {
    setForm({
      name:               profile?.name ?? '',
      phone:              profile?.phone ?? '',
      linkedinUrl:        profile?.linkedinUrl ?? '',
      githubUrl:          profile?.githubUrl ?? '',
      preferredRoles:     profile?.preferredRoles ?? [],
      preferredLocations: profile?.preferredLocations ?? [],
      workModes:          splitComma(profile?.workMode),
      experienceLevels:   splitComma(profile?.experienceLevel),
      expectedSalary:     profile?.expectedSalary ?? '',
      noticePeriod:       profile?.noticePeriod ?? '',
      careerGoals:        profile?.careerGoals ?? [],
    })
    setEditing(true)
    setResumePicker(false)
    setApplyMsg('')
  }

  const handleSave = () => {
    updatePreferences({
      ...form,
      workMode:        joinComma(form.workModes ?? []),
      experienceLevel: joinComma(form.experienceLevels ?? []),
    })
    setEditing(false)
  }

  const handleUseResume = async (resumeId: string) => {
    setApplyMsg('Reading resume…')
    setResumePicker(false)
    const fields = await applyResume(resumeId)
    setForm((prev) => ({
      ...prev,
      ...(fields.name        && { name: fields.name }),
      ...(fields.email       && {}), // email not editable via this form
      ...(fields.phone       && { phone: fields.phone }),
      ...(fields.linkedinUrl && { linkedinUrl: fields.linkedinUrl }),
      ...(fields.githubUrl   && { githubUrl: fields.githubUrl }),
    }))
    setApplyMsg(
      [
        fields.name        && 'name',
        fields.phone       && 'phone',
        fields.linkedinUrl && 'LinkedIn',
        fields.githubUrl   && 'GitHub',
      ]
        .filter(Boolean)
        .join(', ')
        ? `Applied from resume: ${[
            fields.name        && 'name',
            fields.phone       && 'phone',
            fields.linkedinUrl && 'LinkedIn',
            fields.githubUrl   && 'GitHub',
          ].filter(Boolean).join(', ')}`
        : 'No extractable fields found — fill manually.',
    )
  }

  const handleConfirmAll = () => {
    const payload = pendingSkills.map((s) => ({
      skillName:   s.skillName,
      proficiency: s.proficiency ?? 'INTERMEDIATE',
    }))
    confirmSkills(payload)
  }

  if (isLoading) {
    return <div className="flex items-center justify-center h-64 text-gray-400">Loading profile…</div>
  }

  const displayWorkModes  = splitComma(profile?.workMode)
  const displayExpLevels  = splitComma(profile?.experienceLevel)

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Candidate Profile</h1>
          <p className="text-gray-500 mt-1">Your master profile used for job matching.</p>
        </div>
        {!editing && (
          <button onClick={startEdit} className="btn-secondary flex items-center gap-2">
            <Edit2 size={15} /> Edit Preferences
          </button>
        )}
      </div>

      {/* ── Edit Form ── */}
      {editing && (
        <div className="card border-l-4 border-brand-500 space-y-5">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-gray-900">Edit Preferences</h2>
            <button onClick={() => setEditing(false)} className="text-gray-400 hover:text-gray-600">
              <X size={18} />
            </button>
          </div>

          {/* Use Resume section */}
          {resumes.length > 0 && (
            <div className="p-3 bg-blue-50 rounded-lg border border-blue-100">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-sm text-blue-800 font-medium">
                  <FileText size={15} />
                  Use a saved resume to auto-fill fields
                </div>
                <button
                  type="button"
                  onClick={() => setResumePicker((p) => !p)}
                  className="text-xs text-blue-700 underline hover:text-blue-900"
                >
                  {resumePicker ? 'Cancel' : 'Choose resume'}
                </button>
              </div>

              {resumePicker && (
                <div className="mt-3 flex flex-wrap gap-2">
                  {resumes.map((r) => (
                    <button
                      key={r.id}
                      type="button"
                      disabled={isApplyingResume}
                      onClick={() => handleUseResume(r.id)}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-white border border-blue-200 rounded-lg text-sm text-blue-800 hover:bg-blue-100 transition-colors disabled:opacity-50"
                    >
                      <FileText size={13} />
                      <span className="max-w-[160px] truncate">{r.fileName}</span>
                      {r.processed && <CheckCircle size={12} className="text-green-500 shrink-0" />}
                    </button>
                  ))}
                </div>
              )}

              {isApplyingResume && (
                <div className="mt-2 flex items-center gap-2 text-xs text-blue-700">
                  <Loader2 size={12} className="animate-spin" /> Extracting fields from resume…
                </div>
              )}

              {applyMsg && !isApplyingResume && (
                <p className="mt-2 text-xs text-blue-700">{applyMsg}</p>
              )}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="label">Full Name</label>
              <input className="input" value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </div>
            <div>
              <label className="label">Phone</label>
              <input className="input" value={form.phone ?? ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </div>
            <div>
              <label className="label">LinkedIn URL</label>
              <input className="input" value={form.linkedinUrl ?? ''} onChange={(e) => setForm({ ...form, linkedinUrl: e.target.value })} />
            </div>
            <div>
              <label className="label">GitHub URL</label>
              <input className="input" value={form.githubUrl ?? ''} onChange={(e) => setForm({ ...form, githubUrl: e.target.value })} />
            </div>
            <div>
              <label className="label">Expected Salary</label>
              <input className="input" placeholder="e.g. 15–20 LPA" value={form.expectedSalary ?? ''} onChange={(e) => setForm({ ...form, expectedSalary: e.target.value })} />
            </div>
            <div>
              <label className="label">Notice Period</label>
              <input className="input" placeholder="e.g. 30 days" value={form.noticePeriod ?? ''} onChange={(e) => setForm({ ...form, noticePeriod: e.target.value })} />
            </div>
          </div>

          {/* Work Mode multi-toggle */}
          <div>
            <label className="label mb-2">Work Mode <span className="text-gray-400 font-normal text-xs">(select all that apply)</span></label>
            <div className="flex flex-wrap gap-2">
              {WORK_MODES.map((m) => (
                <TogglePill
                  key={m}
                  label={WORK_MODE_LABELS[m]}
                  active={(form.workModes ?? []).includes(m)}
                  onClick={() => setForm({ ...form, workModes: toggle(form.workModes ?? [], m) })}
                />
              ))}
            </div>
          </div>

          {/* Experience Level multi-toggle */}
          <div>
            <label className="label mb-2">Experience Level <span className="text-gray-400 font-normal text-xs">(select all that apply)</span></label>
            <div className="flex flex-wrap gap-2">
              {EXP_LEVELS.map((l) => (
                <TogglePill
                  key={l}
                  label={EXP_LABELS[l]}
                  active={(form.experienceLevels ?? []).includes(l)}
                  onClick={() => setForm({ ...form, experienceLevels: toggle(form.experienceLevels ?? [], l) })}
                />
              ))}
            </div>
            {(form.experienceLevels ?? []).includes('FRESHER') && (
              <p className="text-xs text-gray-400 mt-1.5">
                Fresher — jobs titled "graduate", "associate", "junior", or "entry level" will also be fetched automatically.
              </p>
            )}
          </div>

          <div>
            <label className="label">Preferred Roles <span className="text-gray-400 font-normal text-xs">(press Enter to add)</span></label>
            <TagInput
              value={form.preferredRoles ?? []}
              onChange={(v) => setForm({ ...form, preferredRoles: v })}
              placeholder="e.g. Java Developer"
            />
          </div>
          <div>
            <label className="label">Preferred Locations <span className="text-gray-400 font-normal text-xs">(press Enter to add)</span></label>
            <TagInput
              value={form.preferredLocations ?? []}
              onChange={(v) => setForm({ ...form, preferredLocations: v })}
              placeholder="e.g. Bangalore, Remote"
            />
          </div>
          <div>
            <label className="label">Career Goals <span className="text-gray-400 font-normal text-xs">(press Enter to add)</span></label>
            <TagInput
              value={form.careerGoals ?? []}
              onChange={(v) => setForm({ ...form, careerGoals: v })}
              placeholder="e.g. Lead a backend team"
            />
          </div>

          <div className="flex gap-3 pt-1">
            <button onClick={handleSave} disabled={isSaving} className="btn-primary flex items-center gap-2">
              <Save size={15} /> {isSaving ? 'Saving…' : 'Save'}
            </button>
            <button onClick={() => setEditing(false)} className="btn-secondary">Cancel</button>
          </div>
        </div>
      )}

      {/* ── View Cards ── */}
      {!editing && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="card space-y-3">
            <div className="flex items-center gap-2 text-brand-600 font-semibold mb-1">
              <User size={16} /> Personal Info
            </div>
            <div className="space-y-1 text-sm text-gray-700">
              <p><span className="font-medium">Name:</span> {profile?.name ?? '—'}</p>
              <p><span className="font-medium">Email:</span> {profile?.email ?? '—'}</p>
              <p><span className="font-medium">Phone:</span> {profile?.phone ?? '—'}</p>
              {profile?.linkedinUrl && <p><span className="font-medium">LinkedIn:</span> {profile.linkedinUrl}</p>}
              {profile?.githubUrl   && <p><span className="font-medium">GitHub:</span>   {profile.githubUrl}</p>}
            </div>
          </div>

          <div className="card space-y-3">
            <div className="flex items-center gap-2 text-brand-600 font-semibold mb-1">
              <Briefcase size={16} /> Job Preferences
            </div>
            <div className="space-y-2 text-sm text-gray-700">
              <div>
                <span className="font-medium">Experience:</span>{' '}
                {displayExpLevels.length > 0
                  ? displayExpLevels.map((l) => EXP_LABELS[l] ?? l).join(', ')
                  : '—'}
              </div>
              <div className="flex flex-wrap gap-1.5 items-center">
                <span className="font-medium mr-1">Work Mode:</span>
                {displayWorkModes.length > 0
                  ? displayWorkModes.map((m) => (
                      <span key={m} className="px-2 py-0.5 bg-gray-100 text-gray-700 rounded text-xs">
                        {WORK_MODE_LABELS[m] ?? m}
                      </span>
                    ))
                  : <span>—</span>}
              </div>
              <p><span className="font-medium">Expected Salary:</span> {profile?.expectedSalary ?? '—'}</p>
              <p><span className="font-medium">Notice Period:</span> {profile?.noticePeriod ?? '—'}</p>
            </div>
          </div>

          <div className="card">
            <div className="flex items-center gap-2 text-brand-600 font-semibold mb-3">
              <MapPin size={16} /> Preferred Roles & Locations
            </div>
            <div className="space-y-2">
              <div className="flex flex-wrap gap-2">
                {profile?.preferredRoles.map((r) => <Badge key={r} label={r} variant="default" />)}
                {profile?.preferredRoles.length === 0 && <span className="text-sm text-gray-400">None set — click Edit Preferences</span>}
              </div>
              <div className="flex flex-wrap gap-2">
                {profile?.preferredLocations.map((l) => (
                  <span key={l} className="px-2 py-0.5 bg-blue-50 text-blue-700 rounded text-xs">{l}</span>
                ))}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="flex items-center gap-2 text-brand-600 font-semibold mb-3">
              <Target size={16} /> Career Goals
            </div>
            <div className="flex flex-wrap gap-2">
              {profile?.careerGoals.map((g) => <Badge key={g} label={g} variant="default" />)}
              {profile?.careerGoals.length === 0 && <span className="text-sm text-gray-400">None set</span>}
            </div>
          </div>
        </div>
      )}

      {/* ── Pending Skills ── */}
      {pendingSkills.length > 0 && (
        <div className="card border-l-4 border-orange-400">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-gray-900">Confirm AI-Inferred Skills ({pendingSkills.length})</h3>
            <button onClick={handleConfirmAll} disabled={isConfirming} className="btn-primary text-sm py-1.5">
              {isConfirming ? 'Confirming…' : 'Confirm All'}
            </button>
          </div>
          <p className="text-sm text-gray-500 mb-3">These skills were extracted from your resume by AI.</p>
          <div className="flex flex-wrap gap-2">
            {pendingSkills.map((s) => (
              <span key={s.id} className="px-3 py-1 bg-orange-50 text-orange-800 rounded-full text-sm border border-orange-200">
                {s.skillName}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* ── Confirmed Skills ── */}
      {confirmedSkills.length > 0 && (
        <div className="card">
          <h3 className="font-semibold text-gray-900 mb-3">Confirmed Skills ({confirmedSkills.length})</h3>
          <div className="flex flex-wrap gap-2">
            {confirmedSkills.map((s) => (
              <span key={s.id} className="px-3 py-1 bg-green-50 text-green-800 rounded-full text-sm border border-green-200">
                {s.skillName}
                {s.proficiency && <span className="ml-1 opacity-70 text-xs">· {s.proficiency}</span>}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
