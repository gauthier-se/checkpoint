import { Heart } from 'lucide-react'
import { PlayLogForm } from './play-log-form'
import type { GameDetail } from '@/types/game'
import type { PlayLogDetail } from '@/types/play-log'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useWishlistBacklogActions } from '@/hooks/use-wishlist-backlog-actions'

interface PlayLogDialogProps {
  game: GameDetail
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess?: () => void
  initialPlayLog?: PlayLogDetail
}

export function PlayLogDialog({
  game,
  open,
  onOpenChange,
  onSuccess,
  initialPlayLog,
}: PlayLogDialogProps) {
  const isEdit = !!initialPlayLog
  const { liked, toggleLike, likePending } = useWishlistBacklogActions(game.id)
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {isEdit ? 'Edit Play Session' : 'Log Play Session'}
          </DialogTitle>
          <DialogDescription>
            {isEdit
              ? `Update your play session for ${game.title}.`
              : `Record your playtime, dates, and thoughts for ${game.title}.`}
          </DialogDescription>
        </DialogHeader>

        {/* Like toggle — mark this as a game you love, independent of the play session */}
        <div className="flex items-center justify-between rounded-md border px-3 py-2">
          <span className="text-sm text-muted-foreground">
            Do you love this game?
          </span>
          <Button
            type="button"
            variant={liked ? 'default' : 'outline'}
            size="sm"
            className="gap-2"
            disabled={likePending}
            aria-pressed={liked}
            onClick={() => toggleLike()}
          >
            <Heart className={cn('w-4 h-4', liked && 'fill-current')} />
            {liked ? 'Liked' : 'Like'}
          </Button>
        </div>

        {isEdit ? (
          <PlayLogForm
            mode="edit"
            game={game}
            initialPlayLog={initialPlayLog}
            onCancel={() => onOpenChange(false)}
            onSuccess={() => {
              onSuccess?.()
              onOpenChange(false)
            }}
          />
        ) : (
          <PlayLogForm
            game={game}
            onCancel={() => onOpenChange(false)}
            onSuccess={() => {
              onSuccess?.()
              onOpenChange(false)
            }}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}
