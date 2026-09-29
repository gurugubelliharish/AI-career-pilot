import { clsx } from 'clsx'

type Variant = 'critical' | 'high' | 'medium' | 'low' | 'applied' | 'interview' | 'offer' | 'rejected' | 'saved' | 'default'

const variantClasses: Record<Variant, string> = {
  critical:  'bg-red-100 text-red-800',
  high:      'bg-orange-100 text-orange-800',
  medium:    'bg-yellow-100 text-yellow-800',
  low:       'bg-gray-100 text-gray-700',
  applied:   'bg-blue-100 text-blue-800',
  interview: 'bg-purple-100 text-purple-800',
  offer:     'bg-green-100 text-green-800',
  rejected:  'bg-red-100 text-red-700',
  saved:     'bg-gray-100 text-gray-700',
  default:   'bg-gray-100 text-gray-700',
}

interface BadgeProps {
  label: string
  variant?: Variant
  className?: string
}

export default function Badge({ label, variant = 'default', className }: BadgeProps) {
  return (
    <span className={clsx('inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium', variantClasses[variant], className)}>
      {label}
    </span>
  )
}
