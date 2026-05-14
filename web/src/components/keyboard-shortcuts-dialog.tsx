import { useEffect, useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { KbdHint } from '@/components/ui/kbd'
import { useAuth } from '@/hooks/use-auth'
import { SHORTCUT_GROUPS } from '@/lib/hotkeys'

interface KeyboardShortcutsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

function useIsMac() {
  const [isMac, setIsMac] = useState<boolean | null>(null)
  useEffect(() => {
    setIsMac(/Mac|iPod|iPhone|iPad/.test(navigator.userAgent))
  }, [])
  return isMac
}

export function KeyboardShortcutsDialog({
  open,
  onOpenChange,
}: KeyboardShortcutsDialogProps) {
  const { user } = useAuth()
  const isMac = useIsMac()

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Keyboard shortcuts</DialogTitle>
          <DialogDescription>
            Press these keys outside of input fields to navigate and trigger
            actions.
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-6">
          {SHORTCUT_GROUPS.map((group) => (
            <section key={group.title} className="flex flex-col gap-2">
              <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                {group.title}
              </h3>
              <ul className="flex flex-col gap-1.5">
                {group.items.map((item) => {
                  const disabled = item.authOnly && !user
                  return (
                    <li
                      key={item.keys.join('+')}
                      className={`flex items-center justify-between gap-4 text-sm ${
                        disabled ? 'opacity-50' : ''
                      }`}
                    >
                      <span>{item.label}</span>
                      <KbdHint keys={item.keys} isMac={isMac} />
                    </li>
                  )
                })}
              </ul>
            </section>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  )
}
