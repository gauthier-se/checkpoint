import { Loader2, UserMinus } from 'lucide-react'
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

interface FollowActionButtonProps {
  /** Visible button label, also used as its accessible name. */
  label: string
  /** Confirmation dialog title. */
  title: string
  /** Confirmation dialog description. */
  description: string
  /** Label of the confirming action button inside the dialog. */
  confirmLabel: string
  /** Called when the user confirms the action. */
  onConfirm: () => void
  /** Whether the underlying mutation is in flight. */
  isPending: boolean
}

/**
 * A small destructive action button (Unfollow / Remove follower) guarded by an
 * {@link AlertDialog} confirmation. Shared between the Following and Followers
 * profile tabs so the confirmation flow lives in a single place.
 */
export function FollowActionButton({
  label,
  title,
  description,
  confirmLabel,
  onConfirm,
  isPending,
}: FollowActionButtonProps) {
  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={isPending}
          // Stop the click from bubbling up to the surrounding profile Link.
          onClick={(e) => e.stopPropagation()}
        >
          {isPending ? (
            <Loader2 className="size-3.5 animate-spin" />
          ) : (
            <UserMinus className="size-3.5" />
          )}
          {label}
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent onClick={(e) => e.stopPropagation()}>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isPending}>Cancel</AlertDialogCancel>
          <AlertDialogAction
            onClick={onConfirm}
            disabled={isPending}
            className="bg-destructive text-white hover:bg-destructive/90"
          >
            {confirmLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
