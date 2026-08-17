import { Construction } from 'lucide-react'

interface AdminComingSoonProps {
  /** What this section will manage once it is ported. */
  summary: string
}

/**
 * Placeholder for an admin section whose screens still live in the JavaFX
 * desktop console. Each section replaces it as it is ported.
 */
export function AdminComingSoon({ summary }: AdminComingSoonProps) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed px-6 py-16 text-center">
      <div className="rounded-full bg-muted p-3 text-muted-foreground">
        <Construction className="size-6" />
      </div>
      <p className="font-medium">Not available on the web yet</p>
      <p className="max-w-md text-sm text-muted-foreground">
        {summary} It is still handled by the desktop admin console while this
        section is being ported.
      </p>
    </div>
  )
}
