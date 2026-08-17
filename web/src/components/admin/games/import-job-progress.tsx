import type { ImportJobStatus } from '@/types/admin'
import { Badge } from '@/components/ui/badge'

const STATE_VARIANT = {
  PENDING: 'secondary',
  RUNNING: 'default',
  COMPLETED: 'secondary',
  FAILED: 'destructive',
} as const

/** Share of the fetched batch already processed, 0 when nothing is known yet. */
export function importProgressPercent(job: ImportJobStatus): number {
  if (job.totalFetched <= 0) return 0
  return Math.min(100, Math.round((job.processed / job.totalFetched) * 100))
}

export function ImportJobProgress({ job }: { job: ImportJobStatus }) {
  const percent = importProgressPercent(job)
  const isRunning = job.state === 'PENDING' || job.state === 'RUNNING'

  return (
    <div className="flex flex-col gap-3 rounded-lg border p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <Badge variant={STATE_VARIANT[job.state]}>{job.state}</Badge>
          <span className="text-sm text-muted-foreground">
            {job.type} · limit {job.requestedLimit}
          </span>
        </div>
        <span className="text-sm tabular-nums text-muted-foreground">
          {job.processed}/{job.totalFetched || '?'}
        </span>
      </div>

      <div
        className="h-2 w-full overflow-hidden rounded-full bg-muted"
        role="progressbar"
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label="Import progress"
      >
        <div
          className={
            job.state === 'FAILED'
              ? 'h-full bg-destructive transition-all'
              : 'h-full bg-primary transition-all'
          }
          style={{ width: `${percent}%` }}
        />
      </div>

      <dl className="grid grid-cols-3 gap-2 text-sm">
        <div>
          <dt className="text-xs text-muted-foreground">Imported</dt>
          <dd className="tabular-nums">{job.imported}</dd>
        </div>
        <div>
          <dt className="text-xs text-muted-foreground">Skipped</dt>
          <dd className="tabular-nums">{job.skipped}</dd>
        </div>
        <div>
          <dt className="text-xs text-muted-foreground">Failed</dt>
          <dd className="tabular-nums">{job.failed}</dd>
        </div>
      </dl>

      {isRunning && (
        <p className="text-xs text-muted-foreground">
          Leaving this page does not stop the import — come back and it will
          still be running.
        </p>
      )}

      {job.errorMessage && (
        <p className="text-sm text-destructive">{job.errorMessage}</p>
      )}

      {job.errors.length > 0 && (
        <details className="text-sm">
          <summary className="cursor-pointer text-muted-foreground">
            {job.errors.length} import error
            {job.errors.length === 1 ? '' : 's'}
          </summary>
          <ul className="mt-2 flex max-h-40 flex-col gap-1 overflow-y-auto">
            {job.errors.map((error, index) => (
              <li key={index} className="text-muted-foreground">
                {error}
              </li>
            ))}
          </ul>
        </details>
      )}
    </div>
  )
}
