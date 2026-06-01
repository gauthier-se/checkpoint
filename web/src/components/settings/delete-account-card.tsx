import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { Loader2, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
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
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/hooks/use-auth'
import { deleteAccount } from '@/queries/profile'

interface DeleteAccountCardProps {
  username: string
}

export function DeleteAccountCard({ username }: DeleteAccountCardProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { logout } = useAuth()

  const [open, setOpen] = useState(false)
  const [confirmation, setConfirmation] = useState('')

  const deleteAccountMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: deleteAccount,
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: ['members'] })
      toast.success('Your account has been deleted')
      await logout()
      await navigate({ to: '/login' })
    },
    onError: async () => {
      // Account may already be deleted despite the API error; force logout either way
      queryClient.removeQueries({ queryKey: ['members'] })
      toast.error('Failed to delete account. Please try again.')
      await logout()
      await navigate({ to: '/login' })
    },
  })

  const canConfirm = confirmation === username

  function handleOpenChange(next: boolean) {
    setOpen(next)
    if (!next) {
      setConfirmation('')
    }
  }

  function handleConfirm(event: React.MouseEvent<HTMLButtonElement>) {
    if (!canConfirm) {
      event.preventDefault()
      return
    }
    deleteAccountMutation.mutate()
  }

  return (
    <Card className="border-destructive/50">
      <CardHeader>
        <CardTitle className="text-base text-destructive">
          Danger zone
        </CardTitle>
        <CardDescription>
          Permanently delete your account and all associated data. This action
          cannot be undone.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <AlertDialog open={open} onOpenChange={handleOpenChange}>
          <AlertDialogTrigger asChild>
            <Button type="button" variant="destructive">
              <Trash2 className="size-4" />
              Delete my account
            </Button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Delete your account?</AlertDialogTitle>
              <AlertDialogDescription>
                This will permanently erase your profile, reviews, comments,
                lists, library, follows and every other personal data we hold
                about you. This action cannot be undone.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <Field>
              <FieldLabel htmlFor="delete-account-confirm">
                Type{' '}
                <span className="font-semibold text-foreground">
                  {username}
                </span>{' '}
                to confirm
              </FieldLabel>
              <Input
                id="delete-account-confirm"
                name="delete-account-confirm"
                type="text"
                autoComplete="off"
                value={confirmation}
                onChange={(e) => setConfirmation(e.target.value)}
                disabled={deleteAccountMutation.isPending}
              />
            </Field>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={deleteAccountMutation.isPending}>
                Cancel
              </AlertDialogCancel>
              <AlertDialogAction
                onClick={handleConfirm}
                disabled={!canConfirm || deleteAccountMutation.isPending}
                className="bg-destructive text-white hover:bg-destructive/90"
              >
                {deleteAccountMutation.isPending ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : (
                  <Trash2 className="size-4" />
                )}
                Delete account
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </CardContent>
    </Card>
  )
}
