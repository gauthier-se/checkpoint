import { Gamepad2 } from 'lucide-react'
import { PlayLogForm } from './play-log-form'
import type { GameDetail } from '@/types/game'
import type { PlayLogDetail } from '@/types/play-log'
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
      <DialogContent className="sm:max-w-[700px] md:max-w-[800px] max-h-[90vh] overflow-y-auto">
        <DialogHeader className="flex flex-row items-center space-y-0 gap-3 border-b pb-4 mb-2">
          <DialogTitle>
            {isEdit ? 'Edit Play Session' : 'Log Play Session'}
          </DialogTitle>
          <DialogDescription className="mt-0 pt-0.5">
            —{' '}
            {isEdit
              ? `Update your play session.`
              : `Record your playtime, dates, and thoughts.`}
          </DialogDescription>
        </DialogHeader>

        <div className="grid grid-cols-1 md:grid-cols-[160px_1fr] gap-8 mt-2">
          <div className="flex flex-col gap-3">
            {game.coverUrl ? (
              <img
                src={game.coverUrl}
                alt={game.title}
                className="w-full rounded-lg object-cover aspect-[3/4] shadow-sm"
              />
            ) : (
              <div className="flex w-full items-center justify-center rounded-lg bg-muted aspect-[3/4] shadow-sm">
                <Gamepad2 className="size-10 text-muted-foreground/40" />
              </div>
            )}
            <div>
              <h3 className="font-bold text-lg leading-tight">{game.title}</h3>
              {game.releaseDate && (
                <p className="text-sm text-muted-foreground mt-0.5">
                  {new Date(game.releaseDate).getFullYear()}
                </p>
              )}
            </div>
          </div>

          <div className="flex flex-col min-w-0">
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
                isLiked={liked}
                onToggleLike={toggleLike}
                isLikePending={likePending}
              />
            ) : (
              <PlayLogForm
                game={game}
                onCancel={() => onOpenChange(false)}
                onSuccess={() => {
                  onSuccess?.()
                  onOpenChange(false)
                }}
                isLiked={liked}
                onToggleLike={toggleLike}
                isLikePending={likePending}
              />
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
