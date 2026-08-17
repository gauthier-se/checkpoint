import { Suspense } from 'react'
import { useQueryClient, useSuspenseQuery } from '@tanstack/react-query'
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { ArrowLeft } from 'lucide-react'
import { toast } from 'sonner'
import { AdminGameForm } from '@/components/admin/games/admin-game-form'
import { DeleteGameButton } from '@/components/admin/games/delete-game-button'
import { ErrorPage } from '@/components/errors/error-page'
import { Skeleton } from '@/components/ui/skeleton'
import { toAdminGameFormValues, toAdminGamePayload } from '@/lib/admin-games'
import {
  adminGameDetailQueryOptions,
  adminGamesQueryKey,
  updateAdminGame,
} from '@/queries/admin/games'
import { isApiError } from '@/services/api'

export const Route = createFileRoute(
  '/_app/_protected/admin/games/$gameId/edit',
)({
  loader: ({ context, params }) => {
    void context.queryClient.prefetchQuery(
      adminGameDetailQueryOptions(params.gameId),
    )
  },
  errorComponent: ({ error, reset }) => (
    <ErrorPage
      status={isApiError(error) ? error.status : undefined}
      message={isApiError(error) ? error.message : undefined}
      onRetry={reset}
    />
  ),
  component: EditGamePage,
})

function EditGamePage() {
  return (
    <>
      <Link
        to="/admin/games"
        search={{ page: 1 }}
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        Back to the catalog
      </Link>

      <Suspense fallback={<Skeleton className="h-96 w-full rounded-xl" />}>
        <EditGameContent />
      </Suspense>
    </>
  )
}

function EditGameContent() {
  const { gameId } = Route.useParams()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { data: game } = useSuspenseQuery(adminGameDetailQueryOptions(gameId))

  const backToCatalog = () =>
    navigate({ to: '/admin/games', search: { page: 1 } })

  return (
    <div className="flex flex-col gap-6">
      <AdminGameForm
        defaultValues={toAdminGameFormValues(game)}
        submitLabel="Save changes"
        onSubmit={async (values) => {
          const updated = await updateAdminGame(
            gameId,
            toAdminGamePayload(values),
          )
          toast.success(`“${updated.title}” updated`)
          await queryClient.invalidateQueries({ queryKey: adminGamesQueryKey })
          await queryClient.invalidateQueries({ queryKey: ['games'] })
        }}
      />

      <div className="flex items-center justify-between rounded-lg border border-destructive/50 p-4">
        <div>
          <p className="font-medium text-destructive">Delete this game</p>
          <p className="text-sm text-muted-foreground">
            Only possible while no player references it.
          </p>
        </div>
        <DeleteGameButton
          game={game}
          size="default"
          onDeleted={backToCatalog}
        />
      </div>
    </div>
  )
}
