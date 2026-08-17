import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AdminConfirmButton } from '@/components/admin/admin-confirm-button'
import {
  describeBlockingReferences,
  parseBlockingReferences,
} from '@/lib/admin-games'
import { adminGamesQueryKey, deleteAdminGame } from '@/queries/admin/games'

interface DeleteGameButtonProps {
  game: { id: string; title: string }
  size?: 'sm' | 'default'
  onDeleted?: () => void | Promise<void>
}

/**
 * The API refuses to delete a game that user data still points at, answering
 * `409` with a breakdown of what blocks it. A generic "something went wrong"
 * toast would leave the admin guessing, so the counts are spelled out.
 */
export function DeleteGameButton({
  game,
  size = 'sm',
  onDeleted,
}: DeleteGameButtonProps) {
  const queryClient = useQueryClient()

  return (
    <AdminConfirmButton
      label="Delete"
      size={size}
      title={`Delete “${game.title}”?`}
      description="The game is removed from the catalog permanently. This only works while no player has it in their library, backlog, wishlist or lists."
      mutationFn={() => deleteAdminGame(game.id)}
      onSuccess={async () => {
        toast.success(`“${game.title}” was deleted`)
        await queryClient.invalidateQueries({ queryKey: adminGamesQueryKey })
        await queryClient.invalidateQueries({ queryKey: ['games'] })
        await onDeleted?.()
      }}
      onError={(error) => {
        const blocking = parseBlockingReferences(error)
        if (blocking.length > 0) {
          toast.error(`“${game.title}” is still in use`, {
            description: `Referenced by ${describeBlockingReferences(blocking)}. Remove those first, or keep the game in the catalog.`,
          })
          return
        }
        toast.error(`Could not delete “${game.title}”.`)
      }}
    />
  )
}
