import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link2, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { setReturnToStep } from '../onboarding-store'
import { StepFrame } from '../step-frame'
import { Button } from '@/components/ui/button'
import { authQueryOptions, useAuth } from '@/hooks/use-auth'
import { updateOnboardingStep } from '@/queries/onboarding'
import { apiFetch, isApiError } from '@/services/api'

interface SteamStepProps {
  onNext: () => void
}

type SteamSyncSummary = {
  total: number
  imported: number
  skipped: number
  unmatched: number
}

export function SteamStep({ onNext }: SteamStepProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const syncMutation = useMutation<SteamSyncSummary>({
    meta: { suppressGlobalError: true },
    mutationFn: async () => {
      const res = await apiFetch('/api/me/steam/sync', { method: 'POST' })
      return (await res.json()) as SteamSyncSummary
    },
    onSuccess: async (summary) => {
      toast.success(
        `Imported ${summary.imported} game${summary.imported === 1 ? '' : 's'} from Steam.`,
      )
      await queryClient.invalidateQueries({ queryKey: ['library', 'me'] })
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
      onNext()
    },
    onError: (err) => {
      toast.error(
        isApiError(err) ? err.message : 'Could not sync Steam library.',
      )
    },
  })

  const handleLink = () => {
    setReturnToStep('steam')
    window.location.href = '/api/auth/steam/openid/start?action=link'
  }

  const handleSkip = () => {
    onNext()
    updateOnboardingStep('steam', false)
      .catch(() => {})
      .finally(() => {
        void queryClient.invalidateQueries({
          queryKey: authQueryOptions.queryKey,
        })
      })
  }

  const linkedSteamId = user?.steamId ?? null
  const isLinked = linkedSteamId !== null

  return (
    <StepFrame
      title="Link your Steam account"
      description="Import your owned games in one click and let friends see what you play."
      actions={
        <>
          <Button variant="ghost" onClick={handleSkip}>
            Skip for now
          </Button>
          {isLinked ? (
            <Button
              onClick={() => syncMutation.mutate()}
              disabled={syncMutation.isPending}
            >
              <RefreshCw
                className={`mr-2 size-4 ${syncMutation.isPending ? 'animate-spin' : ''}`}
              />
              {syncMutation.isPending ? 'Syncing...' : 'Sync my library'}
            </Button>
          ) : (
            <Button onClick={handleLink}>
              <Link2 className="mr-2 size-4" />
              Link Steam
            </Button>
          )}
        </>
      }
    >
      {isLinked ? (
        <div className="rounded-md border p-4 text-sm">
          <p className="font-medium">
            {user.steamDisplayName ?? 'Steam account linked'}
          </p>
          <p className="text-muted-foreground font-mono text-xs">
            {linkedSteamId}
          </p>
        </div>
      ) : (
        <p className="text-muted-foreground text-sm">
          You&apos;ll be redirected to Steam to confirm. We&apos;ll bring you
          right back here.
        </p>
      )}
    </StepFrame>
  )
}
