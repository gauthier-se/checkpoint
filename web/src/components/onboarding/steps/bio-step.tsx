import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { StepFrame } from '../step-frame'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { authQueryOptions, useAuth } from '@/hooks/use-auth'
import { updateOnboardingStep } from '@/queries/onboarding'
import { updateProfile } from '@/queries/profile'
import { isApiError } from '@/services/api'

const MAX_BIO = 280

interface BioStepProps {
  onNext: () => void
}

export function BioStep({ onNext }: BioStepProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [value, setValue] = useState(user?.bio ?? '')
  const [isSaving, setIsSaving] = useState(false)

  const handleSave = async () => {
    if (!user) return
    const trimmed = value.trim()
    if (!trimmed) {
      await updateOnboardingStep('bio', false)
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
      onNext()
      return
    }
    setIsSaving(true)
    try {
      await updateProfile({
        pseudo: user.username,
        bio: trimmed,
        isPrivate: user.isPrivate,
      })
      toast.success('Bio saved.')
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
      onNext()
    } catch (err) {
      toast.error(isApiError(err) ? err.message : 'Could not save bio.')
    } finally {
      setIsSaving(false)
    }
  }

  const handleSkip = () => {
    onNext()
    updateOnboardingStep('bio', false)
      .catch(() => {})
      .finally(() => {
        void queryClient.invalidateQueries({
          queryKey: authQueryOptions.queryKey,
        })
      })
  }

  return (
    <StepFrame
      title="Write a short bio"
      description="Tell others what kind of games you love. You can change this any time."
      actions={
        <>
          <Button variant="ghost" onClick={handleSkip}>
            Skip for now
          </Button>
          <Button onClick={handleSave} disabled={isSaving}>
            {isSaving ? 'Saving...' : 'Save & continue'}
          </Button>
        </>
      }
    >
      <Textarea
        value={value}
        onChange={(e) => setValue(e.target.value.slice(0, MAX_BIO))}
        placeholder="JRPG nerd. Currently obsessed with metroidvanias."
        rows={4}
        maxLength={MAX_BIO}
      />
      <p className="text-muted-foreground text-right text-xs">
        {value.length} / {MAX_BIO}
      </p>
    </StepFrame>
  )
}
