import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import type { ImportJobStatus } from '@/types/admin'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ImportJobProgress } from '@/components/admin/games/import-job-progress'
import {
  adminGamesQueryKey,
  importJobQueryOptions,
  startRecentImport,
  startTopRatedImport,
} from '@/queries/admin/games'

export function BulkImportPanel() {
  const queryClient = useQueryClient()
  const [jobId, setJobId] = useState<string | null>(null)
  const [topRatedLimit, setTopRatedLimit] = useState('100')
  const [minRatingCount, setMinRatingCount] = useState('100')
  const [recentLimit, setRecentLimit] = useState('100')

  // Polls until the job reaches a terminal state; the interval is dropped
  // there, so a finished job stops generating requests.
  const { data: job } = useQuery(importJobQueryOptions(jobId))

  const onJobStarted = (started: ImportJobStatus) => {
    setJobId(started.jobId)
    queryClient.setQueryData(
      importJobQueryOptions(started.jobId).queryKey,
      started,
    )
    toast.success('Import started')
  }

  const topRated = useMutation({
    mutationFn: () =>
      startTopRatedImport({
        limit: Number(topRatedLimit) || 100,
        minRatingCount: Number(minRatingCount) || 0,
      }),
    onSuccess: onJobStarted,
  })

  const recent = useMutation({
    mutationFn: () => startRecentImport({ limit: Number(recentLimit) || 100 }),
    onSuccess: onJobStarted,
  })

  const isStarting = topRated.isPending || recent.isPending
  const isRunning = job?.state === 'PENDING' || job?.state === 'RUNNING'

  // Bring the catalog up to date once the job stops.
  const finishedJobId = job && !isRunning ? job.jobId : null
  const [refreshedFor, setRefreshedFor] = useState<string | null>(null)
  if (finishedJobId && finishedJobId !== refreshedFor) {
    setRefreshedFor(finishedJobId)
    void queryClient.invalidateQueries({ queryKey: adminGamesQueryKey })
    void queryClient.invalidateQueries({ queryKey: ['games'] })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Bulk import</CardTitle>
        <CardDescription>
          Pulls many titles from IGDB in the background. Only one import runs at
          a time — the API rejects a second one while the first is going.
        </CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-6">
        <div className="flex flex-wrap items-end gap-3">
          <Field className="w-32">
            <FieldLabel htmlFor="topRatedLimit">Top rated: limit</FieldLabel>
            <Input
              id="topRatedLimit"
              type="number"
              min={1}
              max={5000}
              value={topRatedLimit}
              onChange={(event) => setTopRatedLimit(event.target.value)}
            />
          </Field>
          <Field className="w-40">
            <FieldLabel htmlFor="minRatingCount">Min. rating count</FieldLabel>
            <Input
              id="minRatingCount"
              type="number"
              min={0}
              value={minRatingCount}
              onChange={(event) => setMinRatingCount(event.target.value)}
            />
          </Field>
          <Button
            variant="outline"
            disabled={isStarting || isRunning}
            onClick={() => topRated.mutate()}
          >
            {topRated.isPending && <Loader2 className="size-4 animate-spin" />}
            Import top rated
          </Button>
        </div>

        <div className="flex flex-wrap items-end gap-3 border-t pt-4">
          <Field className="w-32">
            <FieldLabel htmlFor="recentLimit">Recent: limit</FieldLabel>
            <Input
              id="recentLimit"
              type="number"
              min={1}
              max={500}
              value={recentLimit}
              onChange={(event) => setRecentLimit(event.target.value)}
            />
          </Field>
          <Button
            variant="outline"
            disabled={isStarting || isRunning}
            onClick={() => recent.mutate()}
          >
            {recent.isPending && <Loader2 className="size-4 animate-spin" />}
            Import recent releases
          </Button>
        </div>

        {job && <ImportJobProgress job={job} />}
      </CardContent>
    </Card>
  )
}
