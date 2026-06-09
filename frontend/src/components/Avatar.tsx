import { cn } from '@/lib/utils'

// Deterministic cool-tone initials chip from the design (6 blue/teal/indigo pairs).
const PAIRS: [string, string][] = [
  ['#D8E6F6', '#0B5CAB'],
  ['#CFEAF0', '#0B7285'],
  ['#DCE0FA', '#3B43A8'],
  ['#D2ECE2', '#1C6B4A'],
  ['#E2DDF6', '#5B3FA8'],
  ['#D6E8F8', '#155E9C'],
]

function initials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .map((w) => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
}

export function Avatar({
  name,
  src,
  size = 28,
  className,
}: {
  name: string
  src?: string | null
  size?: number
  className?: string
}) {
  if (src) {
    return (
      <img
        src={src}
        alt={name}
        title={name}
        width={size}
        height={size}
        className={cn('shrink-0 rounded-full object-cover', className)}
        style={{ width: size, height: size }}
      />
    )
  }
  const [bg, fg] = PAIRS[(name.charCodeAt(0) || 0) % PAIRS.length]
  return (
    <span
      title={name}
      className={cn('inline-flex shrink-0 select-none items-center justify-center rounded-full', className)}
      style={{
        width: size,
        height: size,
        background: bg,
        color: fg,
        fontSize: size * 0.4,
        fontWeight: 650,
        letterSpacing: '-0.02em',
      }}
    >
      {initials(name)}
    </span>
  )
}
