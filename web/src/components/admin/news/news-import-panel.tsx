import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import type { ImportableNewsSource } from '@/types/admin'
import { IMPORTABLE_NEWS_SOURCES } from '@/types/admin'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { importAdminNews, invalidateNews } from '@/queries/admin/news'

const SOURCE_LABELS: Record<ImportableNewsSource, string> = {
  STEAM: 'Steam',
  RSS: 'RSS feeds',
}

/**
 * `MANUAL` is deliberately absent: the API rejects it, since manual articles
 * are written in the editor rather than pulled from anywhere.
 */
export function NewsImportPanel() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (source: ImportableNewsSource) => importAdminNews(source),
    onSuccess: async (imported, source) => {
      toast.success(
        imported === 0
          ? `Nothing new from ${SOURCE_LABELS[source]}`
          : `Imported ${imported} article${imported === 1 ? '' : 's'} from ${SOURCE_LABELS[source]}`,
      )
      await invalidateNews(queryClient)
    },
  })

  return (
    <Card className="mb-6">
      <CardHeader>
        <CardTitle className="text-base">Import feeds</CardTitle>
        <CardDescription>
          Pulls the latest articles from a source. Imported articles are
          published as they arrive and are not editable here.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-wrap gap-2">
        {IMPORTABLE_NEWS_SOURCES.map((source) => (
          <Button
            key={source}
            variant="outline"
            size="sm"
            disabled={mutation.isPending}
            onClick={() => mutation.mutate(source)}
          >
            {mutation.isPending && mutation.variables === source && (
              <Loader2 className="size-4 animate-spin" />
            )}
            Import from {SOURCE_LABELS[source]}
          </Button>
        ))}
      </CardContent>
    </Card>
  )
}
