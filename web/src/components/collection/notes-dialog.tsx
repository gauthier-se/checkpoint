import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import type { PlayStatus } from '@/types/interaction'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Textarea } from '@/components/ui/textarea'
import { apiFetch } from '@/services/api'

const NOTES_MAX_LENGTH = 2000

interface NotesDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  videoGameId: string
  gameTitle: string
  status: PlayStatus
  initialNotes: string | null
}

export function NotesDialog({
  open,
  onOpenChange,
  videoGameId,
  gameTitle,
  status,
  initialNotes,
}: NotesDialogProps) {
  const queryClient = useQueryClient()
  const [draft, setDraft] = useState(initialNotes ?? '')

  useEffect(() => {
    if (open) setDraft(initialNotes ?? '')
  }, [open, initialNotes])

  const saveMutation = useMutation({
    mutationFn: async () => {
      const trimmed = draft.trim()
      await apiFetch(`/api/me/library/${videoGameId}`, {
        method: 'PUT',
        body: JSON.stringify({
          videoGameId,
          status,
          notes: trimmed === '' ? null : draft,
        }),
        headers: { 'Content-Type': 'application/json' },
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['library', 'me'] })
      void queryClient.invalidateQueries({ queryKey: ['games', videoGameId] })
      onOpenChange(false)
    },
  })

  const tooLong = draft.length > NOTES_MAX_LENGTH
  const counterClass = tooLong ? 'text-destructive' : 'text-muted-foreground'

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Notes — {gameTitle}</DialogTitle>
          <DialogDescription>
            Private notes, visible only to you.
          </DialogDescription>
        </DialogHeader>
        <Textarea
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="Your private notes…"
          rows={8}
          aria-invalid={tooLong}
          autoFocus
        />
        <div className={`text-right text-xs ${counterClass}`}>
          {draft.length}/{NOTES_MAX_LENGTH}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button
            onClick={() => saveMutation.mutate()}
            disabled={tooLong || saveMutation.isPending}
          >
            {saveMutation.isPending ? 'Saving…' : 'Save'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
