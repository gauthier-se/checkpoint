import { useMemo, useState } from 'react'
import { useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2, Lock, Plus, Search } from 'lucide-react'
import { toast } from 'sonner'
import type { GameListCard } from '@/types/list'
import { isApiError } from '@/services/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import {
  addGameToList,
  createList,
  listDetailQueryOptions,
  myListsQueryOptions,
  removeGameFromList,
} from '@/queries/lists'

interface AddToListDialogProps {
  gameId: string
  gameTitle: string
  open: boolean
  onOpenChange: (open: boolean) => void
}

const MAX_LISTS = 100

export function AddToListDialog({
  gameId,
  gameTitle,
  open,
  onOpenChange,
}: AddToListDialogProps) {
  const queryClient = useQueryClient()

  const [filter, setFilter] = useState('')
  const [pendingIds, setPendingIds] = useState<ReadonlySet<string>>(new Set())
  const [membershipOverride, setMembershipOverride] = useState<
    ReadonlyMap<string, boolean>
  >(new Map())

  const [showCreate, setShowCreate] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const [newPrivate, setNewPrivate] = useState(false)
  const [creating, setCreating] = useState(false)

  const { data: listsData, isLoading: isLoadingLists } = useQuery({
    ...myListsQueryOptions(0, MAX_LISTS),
    enabled: open,
  })

  const lists = listsData?.content ?? []

  // Membership is not exposed by any list endpoint, so we read each of the
  // user's lists' details (cached, bounded by how many lists they own) and
  // check whether the current game is among the entries.
  const detailQueries = useQueries({
    queries: lists.map((list) => ({
      ...listDetailQueryOptions(list.id),
      enabled: open,
    })),
  })

  const membershipByListId = useMemo(() => {
    const map = new Map<string, { contains: boolean; loading: boolean }>()
    lists.forEach((list, index) => {
      const query = detailQueries[index]
      map.set(list.id, {
        contains:
          query.data?.entries.some((e) => e.videoGameId === gameId) ?? false,
        loading: query.isLoading,
      })
    })
    return map
  }, [lists, detailQueries, gameId])

  const filteredLists = useMemo(() => {
    const needle = filter.trim().toLowerCase()
    if (!needle) return lists
    return lists.filter((list) => list.title.toLowerCase().includes(needle))
  }, [lists, filter])

  async function invalidateListQueries() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['lists'] }),
      queryClient.invalidateQueries({ queryKey: ['games', gameId, 'lists'] }),
    ])
  }

  function setPending(listId: string, pending: boolean) {
    setPendingIds((prev) => {
      const next = new Set(prev)
      if (pending) {
        next.add(listId)
      } else {
        next.delete(listId)
      }
      return next
    })
  }

  async function handleToggle(list: GameListCard, nextChecked: boolean) {
    setMembershipOverride((prev) => new Map(prev).set(list.id, nextChecked))
    setPending(list.id, true)
    try {
      if (nextChecked) {
        await addGameToList(list.id, gameId)
      } else {
        await removeGameFromList(list.id, gameId)
      }
      await invalidateListQueries()
      toast.success(
        nextChecked
          ? `Added to "${list.title}"`
          : `Removed from "${list.title}"`,
      )
    } catch (err) {
      // Roll back the optimistic toggle.
      setMembershipOverride((prev) => new Map(prev).set(list.id, !nextChecked))
      toast.error(isApiError(err) ? err.message : 'Failed to update list')
    } finally {
      setPending(list.id, false)
    }
  }

  async function handleCreate() {
    const title = newTitle.trim()
    if (!title) return
    setCreating(true)
    try {
      const list = await createList({ title, isPrivate: newPrivate })
      await addGameToList(list.id, gameId)
      setMembershipOverride((prev) => new Map(prev).set(list.id, true))
      await invalidateListQueries()
      toast.success(`Created "${title}" and added ${gameTitle}`)
      setNewTitle('')
      setNewPrivate(false)
      setShowCreate(false)
    } catch (err) {
      toast.error(isApiError(err) ? err.message : 'Failed to create list')
    } finally {
      setCreating(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Add to a list</DialogTitle>
          <DialogDescription>
            Choose which of your lists should contain {gameTitle}.
          </DialogDescription>
        </DialogHeader>

        {lists.length > 0 && (
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
            <Input
              placeholder="Filter your lists..."
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              className="pl-9"
            />
          </div>
        )}

        <div className="max-h-[300px] space-y-1 overflow-y-auto">
          {isLoadingLists && (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="size-5 animate-spin text-muted-foreground" />
            </div>
          )}

          {!isLoadingLists && lists.length === 0 && (
            <p className="py-6 text-center text-sm text-muted-foreground">
              You don&apos;t have any lists yet. Create one below.
            </p>
          )}

          {!isLoadingLists &&
            lists.length > 0 &&
            filteredLists.length === 0 && (
              <p className="py-6 text-center text-sm text-muted-foreground">
                No lists match &quot;{filter}&quot;.
              </p>
            )}

          {filteredLists.map((list) => {
            const membership = membershipByListId.get(list.id)
            const isPending = pendingIds.has(list.id)
            const checked =
              membershipOverride.get(list.id) ?? membership?.contains ?? false
            const checkboxId = `add-to-list-${list.id}`

            return (
              <label
                key={list.id}
                htmlFor={checkboxId}
                className="flex cursor-pointer items-center gap-3 rounded-md p-2 transition-colors hover:bg-muted"
              >
                {isPending ? (
                  <Loader2 className="size-4 shrink-0 animate-spin text-muted-foreground" />
                ) : (
                  <Checkbox
                    id={checkboxId}
                    checked={checked}
                    disabled={membership?.loading ?? true}
                    onCheckedChange={(value) =>
                      handleToggle(list, value === true)
                    }
                  />
                )}
                <div className="min-w-0 flex-1">
                  <p className="flex items-center gap-1.5 truncate text-sm font-medium">
                    {list.title}
                    {list.isPrivate && (
                      <Lock
                        className="size-3 shrink-0 text-muted-foreground"
                        aria-label="Private list"
                      />
                    )}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {list.videoGamesCount}{' '}
                    {list.videoGamesCount === 1 ? 'game' : 'games'}
                  </p>
                </div>
              </label>
            )
          })}
        </div>

        <div className="border-t pt-4">
          {showCreate ? (
            <div className="space-y-3">
              <div className="space-y-2">
                <Label htmlFor="new-list-title">New list title</Label>
                <Input
                  id="new-list-title"
                  placeholder="My game list"
                  value={newTitle}
                  maxLength={100}
                  autoFocus
                  onChange={(e) => setNewTitle(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault()
                      void handleCreate()
                    }
                  }}
                />
              </div>
              <div className="flex items-center gap-2">
                <Switch
                  id="new-list-private"
                  checked={newPrivate}
                  onCheckedChange={setNewPrivate}
                />
                <Label htmlFor="new-list-private" className="cursor-pointer">
                  Private list
                </Label>
              </div>
              <div className="flex justify-end gap-2">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  disabled={creating}
                  onClick={() => {
                    setShowCreate(false)
                    setNewTitle('')
                    setNewPrivate(false)
                  }}
                >
                  Cancel
                </Button>
                <Button
                  type="button"
                  size="sm"
                  disabled={creating || newTitle.trim().length === 0}
                  onClick={() => void handleCreate()}
                >
                  {creating && <Loader2 className="size-4 animate-spin" />}
                  Create &amp; add
                </Button>
              </div>
            </div>
          ) : (
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="w-full gap-2"
              onClick={() => setShowCreate(true)}
            >
              <Plus className="size-4" />
              Create a new list
            </Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
