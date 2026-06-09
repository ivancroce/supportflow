import { cn } from '@/lib/utils'

// "SupportFlow" wordmark with the rounded cloud glyph. `light` renders for the navy sidebar:
// white text + white glyph (blue cloud) and a lighter-blue "Flow".
export function Wordmark({ size = 17, light = false }: { size?: number; light?: boolean }) {
  const glyph = size * 1.6
  return (
    <span className="inline-flex items-center" style={{ gap: 9 }}>
      <span
        className="inline-flex items-center justify-center shadow-soft"
        style={{
          width: glyph,
          height: glyph,
          borderRadius: size * 0.42,
          background: light ? '#fff' : 'var(--brand)',
        }}
      >
        <svg width={size} height={size} viewBox="0 0 24 24" fill={light ? 'var(--brand)' : '#fff'}>
          <path d="M17.4 10.1a4 4 0 0 0-1.2.18 4.7 4.7 0 0 0-8.5-1.2 3.5 3.5 0 0 0-1 .14A3.6 3.6 0 0 0 6.2 16h11.2a3 3 0 0 0 0-5.9z" />
        </svg>
      </span>
      <span
        className={cn('font-bold tracking-[-0.03em]', light ? 'text-white' : 'text-ink')}
        style={{ fontSize: size }}
      >
        Support
        <span style={{ color: light ? '#5BAAED' : 'var(--brand)' }}>Flow</span>
      </span>
    </span>
  )
}
