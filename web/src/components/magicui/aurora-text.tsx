import { memo } from 'react'
import type { CSSProperties, ReactNode } from 'react'

interface AuroraTextProps {
  children: ReactNode
  className?: string
  colors?: Array<string>
  speed?: number
}

export const AuroraText = memo(
  ({
    children,
    className = '',
    colors = ['#7928CA', '#38bdf8', '#FF0080', '#7928CA'],
    speed = 1,
  }: AuroraTextProps) => {
    const gradientStyle: CSSProperties = {
      backgroundImage: `linear-gradient(135deg, ${colors.join(', ')}, ${colors[0]})`,
      WebkitBackgroundClip: 'text',
      WebkitTextFillColor: 'transparent',
      animationDuration: `${10 / speed}s`,
    }

    return (
      <span className={`relative inline-block ${className}`}>
        <span className="sr-only">{children}</span>
        <span
          className="animate-aurora relative bg-[length:200%_auto] bg-clip-text text-transparent"
          style={gradientStyle}
          aria-hidden="true"
        >
          {children}
        </span>
      </span>
    )
  },
)

AuroraText.displayName = 'AuroraText'
