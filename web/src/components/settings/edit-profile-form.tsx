import { useRef, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Camera, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { useAuth } from '@/hooks/use-auth'
import { deletePicture, updateProfile, uploadPicture } from '@/queries/profile'
import { resolvePictureUrl } from '@/lib/picture'

const profileSchema = z.object({
  pseudo: z
    .string()
    .min(1, 'Username is required')
    .max(30, 'Username must not exceed 30 characters'),
  bio: z
    .string()
    .max(500, 'Bio must not exceed 500 characters')
    .nullable()
    .transform((v) => v || null),
  isPrivate: z.boolean(),
})

interface EditProfileFormProps {
  username: string
  bio: string | null
  picture: string | null
  isPrivate: boolean
}

export function EditProfileForm({
  username,
  bio,
  picture,
  isPrivate,
}: EditProfileFormProps) {
  const { invalidate: invalidateAuth } = useAuth()
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [pendingFile, setPendingFile] = useState<File | null>(null)
  const [pictureDeleted, setPictureDeleted] = useState(false)

  const currentPicture = pictureDeleted
    ? null
    : previewUrl || resolvePictureUrl(picture)

  const initials = username.slice(0, 2).toUpperCase()

  const pictureMutation = useMutation({
    mutationFn: uploadPicture,
  })

  const deletePictureMutation = useMutation({
    mutationFn: deletePicture,
  })

  const form = useForm({
    defaultValues: {
      pseudo: username,
      bio: bio ?? '',
      isPrivate: isPrivate,
    },
    validators: {
      onSubmit: profileSchema,
    },
    onSubmit: async ({ value }) => {
      // Update profile info
      const updated = await updateProfile({
        pseudo: value.pseudo,
        bio: value.bio || null,
        isPrivate: value.isPrivate,
      })

      // Handle picture changes
      if (pictureDeleted && !pendingFile) {
        await deletePictureMutation.mutateAsync()
      }

      if (pendingFile) {
        await pictureMutation.mutateAsync(pendingFile)
      }

      // Invalidate queries so profile page reflects changes
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['users'] }),
        invalidateAuth(),
      ])

      // Reset picture state
      setPendingFile(null)
      setPreviewUrl(null)
      setPictureDeleted(false)

      toast.success('Profile updated successfully')
    },
  })

  function handleFileSelect(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return

    // Validate on client side
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
    if (!allowedTypes.includes(file.type)) {
      toast.error('Please select a JPEG, PNG, or WebP image')
      return
    }
    if (file.size > 2 * 1024 * 1024) {
      toast.error('Image must be smaller than 2 MB')
      return
    }

    setPendingFile(file)
    setPictureDeleted(false)
    const reader = new FileReader()
    reader.onload = (e) => setPreviewUrl(e.target?.result as string)
    reader.readAsDataURL(file)

    // Reset input so the same file can be re-selected
    event.target.value = ''
  }

  function handleRemovePicture() {
    setPendingFile(null)
    setPreviewUrl(null)
    setPictureDeleted(true)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        e.stopPropagation()
        form.handleSubmit()
      }}
      className="space-y-6"
    >
      {/* Profile Picture Section */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Profile picture</CardTitle>
          <CardDescription>
            Click to upload a new avatar. JPEG, PNG, or WebP up to 2 MB.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-5">
            <Avatar className="size-20 text-lg">
              <AvatarImage src={currentPicture ?? undefined} />
              <AvatarFallback>{initials}</AvatarFallback>
            </Avatar>
            <div className="flex gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
              >
                <Camera className="mr-2 size-4" />
                Change
              </Button>
              {(currentPicture || picture) && !pictureDeleted && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleRemovePicture}
                >
                  <Trash2 className="mr-2 size-4" />
                  Remove
                </Button>
              )}
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className="hidden"
              onChange={handleFileSelect}
            />
          </div>
        </CardContent>
      </Card>

      {/* Profile Info Section */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Profile information</CardTitle>
          <CardDescription>
            This is how others will see you on Checkpoint
          </CardDescription>
        </CardHeader>
        <CardContent>
          <FieldGroup>
            <form.Field
              name="pseudo"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor="pseudo">Username</FieldLabel>
                  <Input
                    id="pseudo"
                    name="pseudo"
                    type="text"
                    placeholder="Your username"
                    required
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(e) => field.handleChange(e.target.value)}
                  />
                  {field.state.meta.errors.length > 0 && (
                    <p className="text-destructive text-sm">
                      {field.state.meta.errors
                        .map((e) =>
                          typeof e === 'string' ? e : (e as any).message,
                        )
                        .join(', ')}
                    </p>
                  )}
                </Field>
              )}
            />

            <form.Field
              name="bio"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor="bio">Bio</FieldLabel>
                  <Textarea
                    id="bio"
                    name="bio"
                    placeholder="Tell others about yourself..."
                    rows={3}
                    maxLength={500}
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(e) => field.handleChange(e.target.value)}
                  />
                  <p className="text-muted-foreground text-right text-xs">
                    {field.state.value.length}/500
                  </p>
                  {field.state.meta.errors.length > 0 && (
                    <p className="text-destructive text-sm">
                      {field.state.meta.errors
                        .map((e) =>
                          typeof e === 'string' ? e : (e as any).message,
                        )
                        .join(', ')}
                    </p>
                  )}
                </Field>
              )}
            />
          </FieldGroup>
        </CardContent>
      </Card>

      {/* Privacy Section */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Privacy</CardTitle>
          <CardDescription>Control who can see your activity</CardDescription>
        </CardHeader>
        <CardContent>
          <form.Field
            name="isPrivate"
            children={(field) => (
              <div className="flex items-center justify-between gap-4">
                <div className="space-y-0.5">
                  <FieldLabel htmlFor="isPrivate">Private profile</FieldLabel>
                  <p className="text-muted-foreground text-sm">
                    When enabled, only you can see your reviews, wishlist, and
                    activity
                  </p>
                </div>
                <Switch
                  id="isPrivate"
                  checked={field.state.value}
                  onCheckedChange={(checked) => field.handleChange(checked)}
                />
              </div>
            )}
          />
        </CardContent>
      </Card>

      {/* Submit */}
      <div className="flex justify-end">
        <form.Subscribe
          selector={(state) => [state.canSubmit, state.isSubmitting]}
          children={([canSubmit, isSubmitting]) => (
            <Button type="submit" disabled={!canSubmit || isSubmitting}>
              {isSubmitting ? 'Saving...' : 'Save changes'}
            </Button>
          )}
        />
      </div>
    </form>
  )
}
