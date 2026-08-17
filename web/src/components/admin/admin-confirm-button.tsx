import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import type { ReactNode } from 'react'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'

interface AdminConfirmButtonProps {
  label: string
  title: string
  description: ReactNode
  confirmLabel?: string
  variant?: 'destructive' | 'outline' | 'secondary'
  size?: 'sm' | 'default'
  disabled?: boolean
  /** Shown as the button tooltip when `disabled`, to explain why. */
  disabledReason?: string
  mutationFn: () => Promise<unknown>
  onSuccess?: () => void | Promise<void>
}

/**
 * Trigger + confirmation dialog for a moderation action. Every admin action
 * that removes or restricts something goes through this, so the wording and the
 * pending behaviour stay consistent across sections.
 *
 * Failures are reported by the global mutation error toast (see `router.tsx`);
 * the dialog stays open so the action can be retried.
 */
export function AdminConfirmButton({
  label,
  title,
  description,
  confirmLabel,
  variant = 'destructive',
  size = 'sm',
  disabled = false,
  disabledReason,
  mutationFn,
  onSuccess,
}: AdminConfirmButtonProps) {
  const [open, setOpen] = useState(false)

  const mutation = useMutation({
    mutationFn,
    onSuccess: async () => {
      setOpen(false)
      await onSuccess?.()
    },
  })

  if (disabled) {
    return (
      <Button variant={variant} size={size} disabled title={disabledReason}>
        {label}
      </Button>
    )
  }

  return (
    <AlertDialog open={open} onOpenChange={setOpen}>
      <AlertDialogTrigger asChild>
        <Button variant={variant} size={size}>
          {label}
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={mutation.isPending}>
            Cancel
          </AlertDialogCancel>
          <AlertDialogAction
            disabled={mutation.isPending}
            onClick={(event) => {
              // Hold the dialog open until the request settles.
              event.preventDefault()
              mutation.mutate()
            }}
          >
            {mutation.isPending && <Loader2 className="size-4 animate-spin" />}
            {confirmLabel ?? label}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
