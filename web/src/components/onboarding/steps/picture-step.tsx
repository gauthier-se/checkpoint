import { useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ImagePlus } from 'lucide-react'
import { toast } from 'sonner'
import { StepFrame } from '../step-frame'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { resolvePictureUrl } from '@/lib/picture'
import { authQueryOptions, useAuth } from '@/hooks/use-auth'
import { updateOnboardingStep } from '@/queries/onboarding'
import { uploadPicture } from '@/queries/profile'
import { isApiError } from '@/services/api'

interface PictureStepProps {
  onNext: () => void
}

const ACCEPTED = ['image/jpeg', 'image/png', 'image/webp']
const MAX_BYTES = 2 * 1024 * 1024

export function PictureStep({ onNext }: PictureStepProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const inputRef = useRef<HTMLInputElement>(null)
  const [isUploading, setIsUploading] = useState(false)

  const handleFile = async (file: File) => {
    if (!ACCEPTED.includes(file.type)) {
      toast.error('Please upload a JPEG, PNG, or WebP image.')
      return
    }
    if (file.size > MAX_BYTES) {
      toast.error('Image must be smaller than 2MB.')
      return
    }
    setIsUploading(true)
    try {
      await uploadPicture(file)
      toast.success('Profile picture updated.')
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
      onNext()
    } catch (err) {
      toast.error(isApiError(err) ? err.message : 'Could not upload picture.')
    } finally {
      setIsUploading(false)
    }
  }

  const handleSkip = () => {
    onNext()
    updateOnboardingStep('picture', false)
      .catch(() => {})
      .finally(() => {
        void queryClient.invalidateQueries({
          queryKey: authQueryOptions.queryKey,
        })
      })
  }

  const initials = user?.username.slice(0, 2).toUpperCase() ?? '?'

  return (
    <StepFrame
      title="Add a profile picture"
      description="A face (or pixel art) helps friends recognise you in the feed."
      actions={
        <>
          <Button variant="ghost" onClick={handleSkip}>
            Skip for now
          </Button>
          <Button
            onClick={() => inputRef.current?.click()}
            disabled={isUploading}
          >
            <ImagePlus className="mr-2 size-4" />
            {isUploading ? 'Uploading...' : 'Choose an image'}
          </Button>
        </>
      }
    >
      <div className="flex items-center justify-center">
        <Avatar className="size-24">
          <AvatarImage
            src={resolvePictureUrl(user?.picture)}
            alt={user?.username}
          />
          <AvatarFallback>{initials}</AvatarFallback>
        </Avatar>
      </div>
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED.join(',')}
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0]
          if (file) void handleFile(file)
          e.target.value = ''
        }}
      />
    </StepFrame>
  )
}
