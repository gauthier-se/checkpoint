import type { ReactNode } from 'react'
import { Separator } from '@/components/ui/separator'

interface SectionHeaderProps {
  title: string
  /** Optional trailing action, typically a "See all" / "More" link. */
  action?: ReactNode
}

/**
 * Shared title row used by every home page section: a muted heading on the
 * left, an optional action on the right, followed by a separator.
 */
export function SectionHeader({ title, action }: SectionHeaderProps) {
  return (
    <>
      <div className="flex items-center justify-between py-2">
        <h2 className="font-semibold text-muted-foreground">{title}</h2>
        {action}
      </div>
      <Separator />
    </>
  )
}
