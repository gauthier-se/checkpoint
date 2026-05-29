import { useState } from 'react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'

interface FriendAvatarGridProps {
  title: string
  totalCount: number
  visibleLimit?: number
  renderItem: (index: number) => React.ReactNode
  modalRenderItem?: (index: number) => React.ReactNode
}

/**
 * Renders up to {@link FriendAvatarGridProps.visibleLimit} avatars, with a
 * "See all" trigger that opens a modal containing every entry. The caller owns
 * the avatar-tile rendering via {@link FriendAvatarGridProps.renderItem}.
 */
export function FriendAvatarGrid({
  title,
  totalCount,
  visibleLimit = 12,
  renderItem,
  modalRenderItem,
}: FriendAvatarGridProps) {
  const [open, setOpen] = useState(false)
  const visibleCount = Math.min(totalCount, visibleLimit)
  const visible = Array.from({ length: visibleCount }, (_, i) => i)
  const allItems = Array.from({ length: totalCount }, (_, i) => i)
  const showSeeAll = totalCount > visibleLimit

  return (
    <div className="flex flex-wrap items-start gap-x-6 gap-y-4">
      {visible.map((i) => (
        <div key={i}>{renderItem(i)}</div>
      ))}
      {showSeeAll && (
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button
              variant="outline"
              size="sm"
              className="h-12 self-start text-xs"
            >
              See all ({totalCount})
            </Button>
          </DialogTrigger>
          <DialogContent className="max-h-[80vh] overflow-y-auto sm:max-w-2xl">
            <DialogHeader>
              <DialogTitle>{title}</DialogTitle>
            </DialogHeader>
            <div className="flex flex-wrap gap-x-6 gap-y-4 pt-2">
              {allItems.map((i) => (
                <div key={i}>{(modalRenderItem ?? renderItem)(i)}</div>
              ))}
            </div>
          </DialogContent>
        </Dialog>
      )}
    </div>
  )
}
