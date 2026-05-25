import type { ReactNode } from 'react'

interface StepFrameProps {
  title: string
  description?: string
  children: ReactNode
  /** The primary action area (button + optional helper text). */
  actions?: ReactNode
}

/**
 * Shared layout for every wizard step: title, optional description, content
 * area, then action footer. Keeps spacing consistent across steps.
 */
export function StepFrame({
  title,
  description,
  children,
  actions,
}: StepFrameProps) {
  return (
    <div className="flex flex-col gap-6">
      <div className="space-y-1">
        <h2 className="text-xl font-semibold leading-tight">{title}</h2>
        {description && (
          <p className="text-muted-foreground text-sm">{description}</p>
        )}
      </div>
      <div className="space-y-4">{children}</div>
      {actions && (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-end">
          {actions}
        </div>
      )}
    </div>
  )
}
