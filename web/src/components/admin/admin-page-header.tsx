import type { ReactNode } from 'react'

interface AdminPageHeaderProps {
  title: string
  description?: string
  /** Primary action(s) for the section, rendered on the right. */
  actions?: ReactNode
}

/** Consistent heading block for every admin section. */
export function AdminPageHeader({
  title,
  description,
  actions,
}: AdminPageHeaderProps) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
      <div>
        <h2 className="text-xl font-semibold tracking-tight">{title}</h2>
        {description && (
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        )}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  )
}
