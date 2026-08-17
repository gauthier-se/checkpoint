import { useQueryClient } from '@tanstack/react-query'
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { ArrowLeft } from 'lucide-react'
import { toast } from 'sonner'
import { AdminGameForm } from '@/components/admin/games/admin-game-form'
import { EMPTY_GAME_FORM, toAdminGamePayload } from '@/lib/admin-games'
import { adminGamesQueryKey, createAdminGame } from '@/queries/admin/games'

export const Route = createFileRoute('/_app/_protected/admin/games/new')({
  component: NewGamePage,
})

function NewGamePage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return (
    <>
      <BackLink />
      <AdminGameForm
        defaultValues={EMPTY_GAME_FORM}
        submitLabel="Create game"
        onSubmit={async (values) => {
          const created = await createAdminGame(toAdminGamePayload(values))
          toast.success(`“${created.title}” added to the catalog`)
          await queryClient.invalidateQueries({ queryKey: adminGamesQueryKey })
          await queryClient.invalidateQueries({ queryKey: ['games'] })
          await navigate({ to: '/admin/games', search: { page: 1 } })
        }}
      />
    </>
  )
}

function BackLink() {
  return (
    <Link
      to="/admin/games"
      search={{ page: 1 }}
      className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
    >
      <ArrowLeft className="size-4" />
      Back to the catalog
    </Link>
  )
}
