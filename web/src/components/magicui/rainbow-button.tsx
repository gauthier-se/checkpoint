import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { cva } from 'class-variance-authority'
import type { VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const rainbowButtonVariants = cva(
  cn(
    'animate-rainbow group relative inline-flex shrink-0 cursor-pointer items-center justify-center gap-2 rounded-md whitespace-nowrap transition-all',
    'text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50',
    'disabled:pointer-events-none disabled:opacity-50',
    // Soft rainbow glow underneath the button.
    'before:absolute before:bottom-[-20%] before:left-1/2 before:z-0 before:h-1/5 before:w-3/5 before:-translate-x-1/2 before:[background-size:200%] before:[filter:blur(calc(0.8*1rem))] before:animate-rainbow before:bg-[linear-gradient(90deg,var(--color-1),var(--color-5),var(--color-3),var(--color-4),var(--color-2))]',
    '[background-clip:padding-box,border-box,border-box] [background-origin:border-box] [border:calc(0.125*1rem)_solid_transparent] [background-size:200%]',
  ),
  {
    variants: {
      variant: {
        default:
          'bg-[linear-gradient(var(--primary),var(--primary)),linear-gradient(var(--primary)_50%,color-mix(in_oklch,var(--primary)_60%,transparent)_80%,transparent),linear-gradient(90deg,var(--color-1),var(--color-5),var(--color-3),var(--color-4),var(--color-2))] text-primary-foreground',
        outline:
          'border-input bg-[linear-gradient(var(--background),var(--background)),linear-gradient(var(--background)_50%,rgba(18,18,19,0.6)_80%,var(--background)),linear-gradient(90deg,var(--color-1),var(--color-5),var(--color-3),var(--color-4),var(--color-2))] text-foreground',
      },
      size: {
        default: 'h-9 px-4 py-2',
        sm: 'h-8 gap-1.5 px-3',
        lg: 'h-11 px-8 text-base',
        icon: 'size-9',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)

interface RainbowButtonProps
  extends
    React.ComponentProps<'button'>,
    VariantProps<typeof rainbowButtonVariants> {
  asChild?: boolean
}

function RainbowButton({
  className,
  variant,
  size,
  asChild = false,
  ...props
}: RainbowButtonProps) {
  const Comp = asChild ? Slot : 'button'
  return (
    <Comp
      data-slot="rainbow-button"
      className={cn(rainbowButtonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { RainbowButton, rainbowButtonVariants }
