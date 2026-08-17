import { useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import type { ReactNode } from 'react'
import type { AdminNews } from '@/types/admin'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import {
  EMPTY_NEWS_FORM,
  toAdminNewsFormValues,
  toAdminNewsPayload,
} from '@/lib/admin-news'
import {
  createAdminNews,
  invalidateNews,
  updateAdminNews,
} from '@/queries/admin/news'

const newsSchema = z.object({
  title: z.string().min(1, 'A title is required'),
  description: z.string(),
  picture: z.string(),
})

interface NewsEditorDialogProps {
  /** Omit to create a new article. */
  article?: AdminNews
  trigger: ReactNode
}

/**
 * Create/edit dialog, mirroring the desktop `news-editor-dialog`. The endpoint
 * accepts a title, a description and a picture URL — there is no way to set the
 * source, the publication date or a game association here, so none is offered.
 */
export function NewsEditorDialog({ article, trigger }: NewsEditorDialogProps) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)

  const mutation = useMutation({
    mutationFn: (values: typeof EMPTY_NEWS_FORM) =>
      article
        ? updateAdminNews(article.id, toAdminNewsPayload(values))
        : createAdminNews(toAdminNewsPayload(values)),
    onSuccess: async (saved) => {
      toast.success(
        article ? `“${saved.title}” updated` : `“${saved.title}” created`,
      )
      setOpen(false)
      await invalidateNews(queryClient)
    },
  })

  const form = useForm({
    defaultValues: article ? toAdminNewsFormValues(article) : EMPTY_NEWS_FORM,
    validators: { onSubmit: newsSchema },
    onSubmit: async ({ value }) => {
      await mutation.mutateAsync(value)
    },
  })

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next)
        // Drop unsaved edits when the dialog closes.
        if (!next) form.reset()
      }}
    >
      <DialogTrigger asChild>{trigger}</DialogTrigger>

      <DialogContent className="max-w-lg">
        <form
          onSubmit={(event) => {
            event.preventDefault()
            void form.handleSubmit()
          }}
        >
          <DialogHeader>
            <DialogTitle>
              {article ? 'Edit article' : 'New article'}
            </DialogTitle>
            <DialogDescription>
              {article
                ? 'Changes go live immediately for a published article.'
                : 'The article is saved as a draft — publish it when it is ready.'}
            </DialogDescription>
          </DialogHeader>

          <FieldGroup className="my-4">
            <form.Field
              name="title"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor="news-title">Title</FieldLabel>
                  <Input
                    id="news-title"
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                    required
                  />
                  {field.state.meta.errors.length > 0 && (
                    <p className="text-sm text-destructive">
                      {field.state.meta.errors
                        .map((error) =>
                          typeof error === 'string'
                            ? error
                            : ((error as { message?: string }).message ?? ''),
                        )
                        .filter(Boolean)
                        .join(', ')}
                    </p>
                  )}
                </Field>
              )}
            />

            <form.Field
              name="description"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor="news-description">
                    Description
                  </FieldLabel>
                  <Textarea
                    id="news-description"
                    rows={6}
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                  />
                </Field>
              )}
            />

            <form.Field
              name="picture"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor="news-picture">Picture URL</FieldLabel>
                  <Input
                    id="news-picture"
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                  />
                </Field>
              )}
            />
          </FieldGroup>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setOpen(false)}
              disabled={mutation.isPending}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending && (
                <Loader2 className="size-4 animate-spin" />
              )}
              {article ? 'Save changes' : 'Create draft'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
