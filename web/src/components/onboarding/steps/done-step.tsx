import { useQueryClient } from '@tanstack/react-query'
import { PartyPopper } from 'lucide-react'
import { toast } from 'sonner'
import { clearPersistedStep } from '../onboarding-store'
import { StepFrame } from '../step-frame'
import { Button } from '@/components/ui/button'
import { authQueryOptions } from '@/hooks/use-auth'
import { completeOnboarding } from '@/queries/onboarding'
import { isApiError } from '@/services/api'

interface DoneStepProps {
  onClose: () => void
}

export function DoneStep({ onClose }: DoneStepProps) {
  const queryClient = useQueryClient()

  const handleFinish = async () => {
    try {
      await completeOnboarding()
      clearPersistedStep()
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
      onClose()
    } catch (err) {
      toast.error(
        isApiError(err) ? err.message : 'Could not finish onboarding.',
      )
    }
  }

  return (
    <StepFrame
      title="You're all set"
      description="Your CheckPoint account is ready. Enjoy."
      actions={<Button onClick={handleFinish}>Go to my dashboard</Button>}
    >
      <div className="flex flex-col items-center gap-3 py-4">
        <PartyPopper className="text-primary size-10" />
        <p className="text-muted-foreground text-sm">
          You can always tweak any of this from Settings.
        </p>
      </div>
    </StepFrame>
  )
}
