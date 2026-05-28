import { useState } from 'react'
import { Link, createFileRoute } from '@tanstack/react-router'
import { useForm } from '@tanstack/react-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Tag, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import type { Tag as TagType } from '@/types/tag'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { EmptyState } from '@/components/collection/empty-state'
import {
  createTag,
  deleteTag,
  myTagsQueryOptions,
  updateTag,
} from '@/queries/tags'
import { isApiError } from '@/services/api'

const tagNameSchema = z.object({
  name: z
    .string()
    .min(1, 'Tag name is required')
    .max(50, 'Tag name must be 50 characters or less'),
})

export const Route = createFileRoute('/_app/_protected/$username/tags/')({
  component: TagManagementPage,
  loader: async ({ context }) => {
    await context.queryClient.ensureQueryData(myTagsQueryOptions())
  },
})

function TagManagementPage() {
  const { username } = Route.useParams()
  const queryClient = useQueryClient()
  const { data: tags = [] } = useQuery(myTagsQueryOptions())

  const [editingTag, setEditingTag] = useState<TagType | null>(null)
  const [deletingTag, setDeletingTag] = useState<TagType | null>(null)

  const createMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: (name: string) => createTag({ name }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tags', 'me'] })
      toast.success('Tag created')
    },
    onError: (err) =>
      toast.error(isApiError(err) ? err.message : 'Failed to create tag'),
  })

  const renameMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: ({ tagId, name }: { tagId: string; name: string }) =>
      updateTag(tagId, { name }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tags', 'me'] })
      setEditingTag(null)
      toast.success('Tag renamed')
    },
    onError: (err) =>
      toast.error(isApiError(err) ? err.message : 'Failed to rename tag'),
  })

  const deleteMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: (tagId: string) => deleteTag(tagId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tags', 'me'] })
      setDeletingTag(null)
      toast.success('Tag deleted')
    },
    onError: (err) =>
      toast.error(isApiError(err) ? err.message : 'Failed to delete tag'),
  })

  const createForm = useForm({
    defaultValues: { name: '' },
    validators: {
      // @ts-expect-error Form library schema types are slightly off
      onSubmit: tagNameSchema,
    },
    onSubmit: ({ value }) => {
      createMutation.mutate(value.name.trim())
      createForm.reset()
    },
  })

  const renameForm = useForm({
    defaultValues: { name: editingTag?.name ?? '' },
    validators: {
      // @ts-expect-error Form library schema types are slightly off
      onSubmit: tagNameSchema,
    },
    onSubmit: ({ value }) => {
      if (editingTag) {
        renameMutation.mutate({ tagId: editingTag.id, name: value.name.trim() })
      }
    },
  })

  function openRename(tag: TagType) {
    setEditingTag(tag)
    renameForm.reset()
    renameForm.setFieldValue('name', tag.name)
  }

  return (
    <main className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="mb-8 text-3xl font-bold">My Tags</h1>

      {/* Create form */}
      <form
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          void createForm.handleSubmit()
        }}
        className="mb-8 flex gap-2"
      >
        <createForm.Field
          name="name"
          children={(field) => (
            <Input
              placeholder="New tag name..."
              value={field.state.value}
              onChange={(e) => field.handleChange(e.target.value)}
              maxLength={50}
              className="max-w-xs"
            />
          )}
        />
        <createForm.Subscribe
          selector={(s) => [s.canSubmit, s.isSubmitting]}
          children={([canSubmit, isSubmitting]) => (
            <Button
              type="submit"
              disabled={!canSubmit || isSubmitting || createMutation.isPending}
              className="gap-1"
            >
              <Plus className="size-4" />
              Create
            </Button>
          )}
        />
      </form>

      {/* Tag list */}
      {tags.length === 0 ? (
        <EmptyState
          icon={<Tag className="size-12" />}
          title="No tags yet"
          description="Create your first tag to organize your play log entries!"
        />
      ) : (
        <div className="space-y-2">
          {tags.map((tag) => (
            <div
              key={tag.id}
              className="group flex items-center justify-between rounded-lg border bg-card p-4 shadow-sm transition-shadow hover:shadow-md"
            >
              <div className="flex items-center gap-3">
                <Tag className="size-4 text-muted-foreground" />
                <Link
                  to="/profile/$username"
                  params={{ username }}
                  search={{ tab: 'tags', tagName: tag.name, page: 1 }}
                  className="font-medium hover:underline"
                >
                  {tag.name}
                </Link>
                <Badge variant="secondary" className="text-xs">
                  {tag.playLogsCount}{' '}
                  {tag.playLogsCount === 1 ? 'play log' : 'play logs'}
                </Badge>
              </div>
              <div className="flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 gap-1 text-xs"
                  onClick={() => openRename(tag)}
                >
                  <Pencil className="size-3" />
                  Rename
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 gap-1 text-xs text-destructive hover:text-destructive"
                  onClick={() => setDeletingTag(tag)}
                >
                  <Trash2 className="size-3" />
                  Delete
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Rename dialog */}
      <Dialog
        open={editingTag !== null}
        onOpenChange={(open) => {
          if (!open) setEditingTag(null)
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Rename Tag</DialogTitle>
            <DialogDescription>
              Enter a new name for the tag &quot;{editingTag?.name}&quot;.
            </DialogDescription>
          </DialogHeader>
          <form
            onSubmit={(e) => {
              e.preventDefault()
              e.stopPropagation()
              void renameForm.handleSubmit()
            }}
          >
            <div className="space-y-2 py-4">
              <renameForm.Field
                name="name"
                children={(field) => (
                  <div className="space-y-2">
                    <Label htmlFor="edit-tag-name">Name</Label>
                    <Input
                      id="edit-tag-name"
                      value={field.state.value}
                      onChange={(e) => field.handleChange(e.target.value)}
                      maxLength={50}
                    />
                    {field.state.meta.errors.length > 0 ? (
                      <p className="text-sm text-destructive">
                        {field.state.meta.errors
                          .map((e) =>
                            typeof e === 'string' ? e : (e as any).message,
                          )
                          .join(', ')}
                      </p>
                    ) : null}
                  </div>
                )}
              />
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                type="button"
                onClick={() => setEditingTag(null)}
              >
                Cancel
              </Button>
              <renameForm.Subscribe
                selector={(s) => [s.canSubmit, s.isSubmitting]}
                children={([canSubmit, isSubmitting]) => (
                  <Button
                    type="submit"
                    disabled={
                      !canSubmit || isSubmitting || renameMutation.isPending
                    }
                  >
                    {renameMutation.isPending ? 'Saving...' : 'Save'}
                  </Button>
                )}
              />
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete confirmation */}
      <AlertDialog
        open={deletingTag !== null}
        onOpenChange={(open) => {
          if (!open) setDeletingTag(null)
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Tag</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete the tag &quot;{deletingTag?.name}
              &quot;? This will remove it from all play logs. This action cannot
              be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={deleteMutation.isPending}
              onClick={() => {
                if (deletingTag) deleteMutation.mutate(deletingTag.id)
              }}
            >
              {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </main>
  )
}
