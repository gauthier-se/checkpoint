import { Link } from '@tanstack/react-router'
import type { AdminCatalogSearchParams } from '@/types/admin'
import type { Game, PaginationMetadata } from '@/types/game'
import type { AdminDataTableColumn } from '@/components/admin/admin-data-table'
import { AdminDataTable } from '@/components/admin/admin-data-table'
import { DeleteGameButton } from '@/components/admin/games/delete-game-button'

function releaseYear(game: Game): string {
  return game.releaseDate ? game.releaseDate.slice(0, 4) : '-'
}

const columns: Array<AdminDataTableColumn<Game>> = [
  {
    id: 'cover',
    header: <span className="sr-only">Cover</span>,
    className: 'w-14',
    cell: (game) =>
      game.coverUrl ? (
        <img
          src={game.coverUrl}
          alt=""
          className="h-14 w-10 rounded object-cover"
          loading="lazy"
        />
      ) : (
        <div className="h-14 w-10 rounded bg-muted" />
      ),
  },
  {
    id: 'title',
    header: 'Title',
    cell: (game) => <span className="font-medium">{game.title}</span>,
  },
  {
    id: 'releaseDate',
    header: 'Released',
    className: 'w-24',
    cell: (game) => (
      <span className="text-muted-foreground">{releaseYear(game)}</span>
    ),
  },
  {
    id: 'rating',
    header: 'Rating',
    className: 'w-28',
    cell: (game) => (
      <span className="text-muted-foreground tabular-nums">
        {game.averageRating === null
          ? '-'
          : `${game.averageRating.toFixed(1)} (${game.ratingCount})`}
      </span>
    ),
  },
  {
    id: 'actions',
    header: <span className="sr-only">Actions</span>,
    className: 'w-36 text-right',
    cell: (game) => (
      <div className="flex items-center justify-end gap-2">
        <Link
          to="/admin/games/$gameId/edit"
          params={{ gameId: game.id }}
          className="text-sm underline-offset-4 hover:underline"
        >
          Edit
        </Link>
        <DeleteGameButton game={game} />
      </div>
    ),
  },
]

interface AdminGamesTableProps {
  rows: Array<Game>
  metadata: PaginationMetadata
  search: AdminCatalogSearchParams
  isLoading?: boolean
}

export function AdminGamesTable({
  rows,
  metadata,
  search,
  isLoading,
}: AdminGamesTableProps) {
  return (
    <AdminDataTable
      caption="Game catalog"
      columns={columns}
      rows={rows}
      rowKey={(game) => game.id}
      page={search.page}
      metadata={metadata}
      linkProps={(page) => ({
        to: '/admin/games',
        search: { ...search, page },
      })}
      isLoading={isLoading}
      emptyMessage={
        search.q
          ? `No game matches “${search.q}”.`
          : 'The catalog is empty. Import games to get started.'
      }
    />
  )
}
