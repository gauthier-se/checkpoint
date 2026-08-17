import { createFileRoute } from '@tanstack/react-router'
import { BulkImportPanel } from '@/components/admin/games/bulk-import-panel'
import { ExternalGameSearch } from '@/components/admin/games/external-game-search'

export const Route = createFileRoute('/_app/_protected/admin/games/import')({
  component: AdminGameImportPage,
})

function AdminGameImportPage() {
  return (
    <div className="flex flex-col gap-6">
      <ExternalGameSearch />
      <BulkImportPanel />
    </div>
  )
}
