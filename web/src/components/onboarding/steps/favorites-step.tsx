import { useQuery, useQueryClient } from '@tanstack/react-query'
import { StepFrame } from '../step-frame'
import { Button } from '@/components/ui/button'
import { FavoriteGamesEditor } from '@/components/settings/favorite-games-editor'
import { authQueryOptions, useAuth } from '@/hooks/use-auth'
import { updateOnboardingStep } from '@/queries/onboarding'
import { userProfileQueryOptions } from '@/queries/profile'

interface FavoritesStepProps {
  onNext: () => void
}

export function FavoritesStep({ onNext }: FavoritesStepProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  // Load current favorites so re-entering the step doesn't wipe earlier picks.
  const profileQuery = useQuery({
    ...userProfileQueryOptions(user?.username ?? ''),
    enabled: !!user,
  })

  const handleSkip = () => {
    onNext()
    updateOnboardingStep('favorites', false)
      .catch(() => {})
      .finally(() => {
        void queryClient.invalidateQueries({
          queryKey: authQueryOptions.queryKey,
        })
      })
  }

  return (
    <StepFrame
      title="Pick your favorite games"
      description="Feature up to 5 games on your profile. Search to add, drag to reorder."
      actions={
        <>
          <Button variant="ghost" onClick={handleSkip}>
            Skip for now
          </Button>
          <Button onClick={onNext}>Continue</Button>
        </>
      }
    >
      {user && profileQuery.data ? (
        <FavoriteGamesEditor
          embedded
          username={user.username}
          initialFavorites={profileQuery.data.favorites}
          onSaved={() =>
            queryClient.invalidateQueries({
              queryKey: authQueryOptions.queryKey,
            })
          }
        />
      ) : (
        <p className="text-muted-foreground text-sm">Loading your library...</p>
      )}
    </StepFrame>
  )
}
