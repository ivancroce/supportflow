import { cva, type VariantProps } from 'class-variance-authority'
import { Loader2 } from 'lucide-react'
import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { cn } from '@/lib/utils'

// SLDS button, ported from the design handoff (variants primary/secondary/ghost/soft/danger,
// sizes sm/md/lg). Kept separate from the shadcn base-ui button so the queue UI matches the spec.
const button = cva(
  'inline-flex shrink-0 items-center justify-center font-semibold tracking-[-0.01em] whitespace-nowrap ' +
    'transition-[background-color,border-color,transform] duration-150 outline-none ' +
    'focus-visible:ring-2 focus-visible:ring-brand/50 active:scale-[0.98] ' +
    'disabled:pointer-events-none disabled:opacity-55',
  {
    variants: {
      variant: {
        primary: 'bg-brand text-white border border-brand-press shadow-soft hover:bg-brand-hover',
        secondary:
          'bg-card text-ink border border-hairline-strong shadow-soft hover:bg-card-muted hover:border-ink-4',
        ghost: 'bg-transparent text-ink-2 border border-transparent hover:bg-canvas-deep hover:text-ink',
        soft: 'bg-brand-soft text-brand-ink border border-transparent hover:bg-[#d9ecfb]',
        danger:
          'bg-card text-danger-ink border border-hairline-strong shadow-soft hover:bg-danger-soft hover:border-danger',
      },
      size: {
        sm: 'h-8 gap-1.5 rounded-md px-3 text-[13px]',
        md: 'h-[38px] gap-1.5 rounded-[9px] px-[15px] text-[13.5px]',
        lg: 'h-[46px] gap-2 rounded-[11px] px-[18px] text-[15px]',
      },
      full: { true: 'w-full', false: '' },
    },
    defaultVariants: { variant: 'primary', size: 'md', full: false },
  },
)

type IconType = React.ComponentType<{ className?: string; size?: number | string }>

interface ButtonProps
  extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'>,
    VariantProps<typeof button> {
  icon?: IconType
  iconRight?: IconType
  loading?: boolean
  children?: ReactNode
}

export function Button({
  variant,
  size,
  full,
  icon: Icon,
  iconRight: IconRight,
  loading,
  className,
  children,
  disabled,
  ...rest
}: ButtonProps) {
  const iconSize = size === 'lg' ? 18 : size === 'sm' ? 15 : 16
  return (
    <button
      className={cn(button({ variant, size, full }), className)}
      disabled={disabled || loading}
      {...rest}
    >
      {loading ? (
        <Loader2 size={iconSize} className="animate-spin" />
      ) : (
        Icon && <Icon size={iconSize} />
      )}
      {children}
      {IconRight && !loading && <IconRight size={iconSize} />}
    </button>
  )
}
