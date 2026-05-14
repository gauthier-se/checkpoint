import { Command } from 'lucide-react'
import { cn } from '@/lib/utils'

interface KbdHintProps {
  keys: ReadonlyArray<string>
  isMac?: boolean | null
  className?: string
}

const KEY_BASE_CLASS =
  'pointer-events-none inline-flex select-none items-center gap-0.5 rounded bg-muted px-1.5 py-0.5 font-mono text-[11px] font-medium text-muted-foreground'

function renderKey(key: string, isMac: boolean | null | undefined) {
  if (key === 'Mod') {
    if (isMac === null || isMac === undefined) return <span>Ctrl</span>
    return isMac ? <Command className="size-2.5" /> : <span>Ctrl</span>
  }
  return <span>{key}</span>
}

export function KbdHint({ keys, isMac, className }: KbdHintProps) {
  return (
    <span className={cn('inline-flex items-center gap-1', className)}>
      {keys.map((key, index) => (
        <kbd key={`${key}-${index}`} className={KEY_BASE_CLASS}>
          {renderKey(key, isMac)}
        </kbd>
      ))}
    </span>
  )
}
