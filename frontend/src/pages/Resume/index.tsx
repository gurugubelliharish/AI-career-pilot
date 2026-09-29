import { useDropzone } from 'react-dropzone'
import { Upload, FileText, CheckCircle, AlertCircle, ExternalLink } from 'lucide-react'
import { clsx } from 'clsx'
import { useResumeViewModel } from '@/viewmodels/useResumeViewModel'

function fileIcon(fileType?: string) {
  if (fileType === 'application/pdf') return '📄'
  if (fileType?.includes('word')) return '📝'
  return '📋'
}

export default function Resume() {
  const { resumes, uploadState, message, isProcessing, onFileDrop, reset, openResume } =
    useResumeViewModel()

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: onFileDrop,
    accept: {
      'application/pdf': ['.pdf'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
    },
    maxFiles: 1,
    disabled: isProcessing,
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Resume Upload</h1>
        <p className="text-gray-500 mt-1">Upload your resume — AI will extract your skills automatically.</p>
      </div>

      {/* Drop zone */}
      <div
        {...getRootProps()}
        className={clsx(
          'card border-2 border-dashed cursor-pointer transition-colors text-center py-12',
          isDragActive ? 'border-brand-500 bg-brand-50' : 'border-gray-300 hover:border-brand-400',
          isProcessing && 'opacity-50 cursor-wait',
        )}
      >
        <input {...getInputProps()} />
        <Upload size={40} className="mx-auto text-gray-400 mb-3" />
        <p className="text-gray-700 font-medium">
          {isDragActive ? 'Drop your resume here' : 'Drag & drop your resume, or click to browse'}
        </p>
        <p className="text-sm text-gray-400 mt-1">PDF or DOCX · Max 20 MB</p>
      </div>

      {/* Upload status */}
      {message && (
        <div className={clsx('flex items-center gap-3 p-4 rounded-lg', {
          'bg-blue-50 text-blue-800':   uploadState === 'uploading' || uploadState === 'extracting',
          'bg-green-50 text-green-800': uploadState === 'done',
          'bg-red-50 text-red-800':     uploadState === 'error',
        })}>
          {uploadState === 'done'  && <CheckCircle size={18} />}
          {uploadState === 'error' && <AlertCircle size={18} />}
          {isProcessing && <FileText size={18} className="animate-pulse" />}
          <span className="text-sm flex-1">{message}</span>
          {(uploadState === 'done' || uploadState === 'error') && (
            <button onClick={reset} className="text-xs underline opacity-70 hover:opacity-100">
              Upload another
            </button>
          )}
        </div>
      )}

      {/* Uploaded resumes */}
      {resumes.length > 0 && (
        <div className="space-y-3">
          <h2 className="text-lg font-semibold text-gray-900">
            Your Resumes ({resumes.length})
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {resumes.map((resume) => (
              <div
                key={resume.id}
                role="button"
                tabIndex={0}
                onClick={() => openResume(resume.id, resume.fileName)}
                onKeyDown={(e) => e.key === 'Enter' && openResume(resume.id, resume.fileName)}
                className="card hover:shadow-md transition-shadow cursor-pointer group border border-transparent hover:border-brand-200"
              >
                {/* File placeholder */}
                <div className="flex items-center justify-center h-24 bg-gray-50 rounded-lg mb-3 text-4xl group-hover:bg-brand-50 transition-colors">
                  {fileIcon(resume.fileType)}
                </div>

                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="font-medium text-gray-900 truncate text-sm" title={resume.fileName}>
                      {resume.fileName}
                    </p>
                    <p className="text-xs text-gray-400 mt-0.5">
                      {new Date(resume.createdAt).toLocaleDateString()}
                    </p>
                    {resume.processed && (
                      <span className="inline-flex items-center gap-1 mt-1.5 px-2 py-0.5 bg-green-50 text-green-700 rounded-full text-xs">
                        <CheckCircle size={11} /> Skills extracted
                      </span>
                    )}
                    {!resume.processed && (
                      <span className="inline-flex items-center mt-1.5 px-2 py-0.5 bg-yellow-50 text-yellow-700 rounded-full text-xs">
                        Processing…
                      </span>
                    )}
                  </div>
                  <ExternalLink size={15} className="text-gray-400 group-hover:text-brand-500 shrink-0 mt-0.5 transition-colors" />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
