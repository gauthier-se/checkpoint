import { useEffect, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { X } from 'lucide-react'
import type {
  GameListSortOption,
  GameListVisibility,
  GameListsSearchParams,
} from '@/types/list'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useAuth } from '@/hooks/use-auth'

const ALL_VALUE = '__all__'
const MIN_GAMES_OPTIONS = [5, 10, 20] as const

type SortOption = { value: GameListSortOption; label: string }

const SORT_OPTIONS: ReadonlyArray<SortOption> = [
  { value: 'recent', label: 'Recent' },
  { value: 'popular', label: 'Popular' },
  { value: 'most-games', label: 'Most games' },
]

interface ListsFiltersProps {
  search: GameListsSearchParams
}

export function ListsFilters({ search }: ListsFiltersProps) {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAuthenticated = user != null

  const [qInput, setQInput] = useState(search.q ?? '')
  const [authorInput, setAuthorInput] = useState(search.author ?? '')

  useEffect(() => {
    setQInput(search.q ?? '')
    setAuthorInput(search.author ?? '')
  }, [search.q, search.author])

  function updateFilter(updates: Partial<GameListsSearchParams>) {
    navigate({
      to: '/lists/browse',
      search: (prev) => ({ ...prev, ...updates, page: 1 }),
      replace: true,
    })
  }

  function applyQuery() {
    const trimmed = qInput.trim()
    const next = trimmed === '' ? undefined : trimmed
    if (next !== search.q) {
      updateFilter({ q: next })
    }
  }

  function applyAuthor() {
    const trimmed = authorInput.trim()
    const next = trimmed === '' ? undefined : trimmed
    if (next !== search.author) {
      updateFilter({ author: next })
    }
  }

  const hasActiveFilters =
    search.q != null ||
    search.sort != null ||
    search.visibility != null ||
    search.author != null ||
    search.minGames != null

  function clearAllFilters() {
    setQInput('')
    setAuthorInput('')
    navigate({ to: '/lists/browse', search: { page: 1 } })
  }

  const activeFilterBadges: Array<{ label: string; key: string }> = []
  if (search.q)
    activeFilterBadges.push({ label: `Search: "${search.q}"`, key: 'q' })
  if (search.sort) {
    const sortLabel = SORT_OPTIONS.find((o) => o.value === search.sort)?.label
    if (sortLabel)
      activeFilterBadges.push({ label: `Sort: ${sortLabel}`, key: 'sort' })
  }
  if (search.visibility === 'mine')
    activeFilterBadges.push({ label: 'Mine only', key: 'visibility' })
  if (search.author)
    activeFilterBadges.push({
      label: `Author: ${search.author}`,
      key: 'author',
    })
  if (search.minGames)
    activeFilterBadges.push({
      label: `≥ ${search.minGames} games`,
      key: 'minGames',
    })

  function removeFilter(key: string) {
    if (key === 'q') setQInput('')
    if (key === 'author') setAuthorInput('')
    updateFilter({ [key]: undefined })
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-3 flex-wrap">
        <Input
          type="search"
          placeholder="Search lists…"
          value={qInput}
          onChange={(e) => setQInput(e.target.value)}
          onBlur={applyQuery}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              applyQuery()
            }
          }}
          className="h-8 w-56 text-sm"
        />

        <Select
          value={search.sort ?? 'recent'}
          onValueChange={(v) =>
            updateFilter({
              sort: v === 'recent' ? undefined : (v as GameListSortOption),
            })
          }
        >
          <SelectTrigger size="sm">
            <SelectValue placeholder="Sort by" />
          </SelectTrigger>
          <SelectContent>
            {SORT_OPTIONS.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {isAuthenticated && (
          <Select
            value={search.visibility ?? 'public'}
            onValueChange={(v) =>
              updateFilter({
                visibility:
                  v === 'public' ? undefined : (v as GameListVisibility),
              })
            }
          >
            <SelectTrigger size="sm">
              <SelectValue placeholder="Visibility" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="public">All public</SelectItem>
              <SelectItem value="mine">Mine</SelectItem>
            </SelectContent>
          </Select>
        )}

        <Input
          type="text"
          placeholder="Author pseudo"
          value={authorInput}
          onChange={(e) => setAuthorInput(e.target.value)}
          onBlur={applyAuthor}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              applyAuthor()
            }
          }}
          className="h-8 w-40 text-sm"
        />

        <Select
          value={
            search.minGames !== undefined ? String(search.minGames) : ALL_VALUE
          }
          onValueChange={(v) =>
            updateFilter({
              minGames: v === ALL_VALUE ? undefined : Number(v),
            })
          }
        >
          <SelectTrigger size="sm">
            <SelectValue placeholder="Min games" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_VALUE}>Any size</SelectItem>
            {MIN_GAMES_OPTIONS.map((min) => (
              <SelectItem key={min} value={String(min)}>
                ≥ {min} games
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {hasActiveFilters && (
        <div className="flex items-center gap-2 flex-wrap">
          {activeFilterBadges.map((badge) => (
            <Badge key={badge.key} variant="secondary" className="gap-1">
              {badge.label}
              <button
                type="button"
                onClick={() => removeFilter(badge.key)}
                className="hover:text-foreground"
              >
                <X className="size-3" />
              </button>
            </Badge>
          ))}
          <Button variant="ghost" size="sm" onClick={clearAllFilters}>
            Clear all
          </Button>
        </div>
      )}
    </div>
  )
}
