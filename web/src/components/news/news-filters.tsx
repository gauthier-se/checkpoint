import { useEffect, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { X } from 'lucide-react'
import type {
  NewsListSearchParams,
  NewsSortOption,
  NewsSource,
} from '@/types/news'
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

const ALL_VALUE = '__all__'

const SOURCE_OPTIONS: Array<{ value: NewsSource; label: string }> = [
  { value: 'MANUAL', label: 'Editorial' },
  { value: 'STEAM', label: 'Steam' },
  { value: 'RSS', label: 'RSS feeds' },
]

const FEED_OPTIONS = [
  'IGN',
  'Eurogamer',
  'Rock Paper Shotgun',
  'Steam Community',
] as const

type SortOption = { value: NewsSortOption; label: string }

const SORT_OPTIONS: ReadonlyArray<SortOption> = [
  { value: 'publishedAt,desc', label: 'Newest first' },
  { value: 'publishedAt,asc', label: 'Oldest first' },
  { value: 'title,asc', label: 'Title (A–Z)' },
  { value: 'title,desc', label: 'Title (Z–A)' },
  { value: 'relevance', label: 'Best match' },
]

interface NewsFiltersProps {
  search: NewsListSearchParams
}

export function NewsFilters({ search }: NewsFiltersProps) {
  const navigate = useNavigate()

  const [qInput, setQInput] = useState(search.q ?? '')
  const [publishedFrom, setPublishedFrom] = useState(search.publishedFrom ?? '')
  const [publishedTo, setPublishedTo] = useState(search.publishedTo ?? '')

  useEffect(() => {
    setQInput(search.q ?? '')
    setPublishedFrom(search.publishedFrom ?? '')
    setPublishedTo(search.publishedTo ?? '')
  }, [search.q, search.publishedFrom, search.publishedTo])

  function updateFilter(updates: Partial<NewsListSearchParams>) {
    navigate({
      to: '/news',
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

  function applyDate(key: 'publishedFrom' | 'publishedTo', value: string) {
    const next = value === '' ? undefined : value
    if (next !== search[key]) {
      updateFilter({ [key]: next })
    }
  }

  const hasQuery = (search.q ?? '').trim().length > 0

  const hasActiveFilters =
    search.q != null ||
    search.source != null ||
    search.feedName != null ||
    search.videoGameId != null ||
    search.publishedFrom != null ||
    search.publishedTo != null ||
    search.sort != null

  function clearAllFilters() {
    setQInput('')
    setPublishedFrom('')
    setPublishedTo('')
    navigate({ to: '/news', search: { page: 1 } })
  }

  const activeFilterBadges: Array<{ label: string; key: string }> = []
  if (search.q)
    activeFilterBadges.push({ label: `Search: "${search.q}"`, key: 'q' })
  if (search.source) {
    const sourceLabel =
      SOURCE_OPTIONS.find((o) => o.value === search.source)?.label ??
      search.source
    activeFilterBadges.push({ label: `Source: ${sourceLabel}`, key: 'source' })
  }
  if (search.feedName)
    activeFilterBadges.push({
      label: `Feed: ${search.feedName}`,
      key: 'feedName',
    })
  if (search.publishedFrom)
    activeFilterBadges.push({
      label: `From: ${search.publishedFrom}`,
      key: 'publishedFrom',
    })
  if (search.publishedTo)
    activeFilterBadges.push({
      label: `To: ${search.publishedTo}`,
      key: 'publishedTo',
    })
  if (search.sort) {
    const sortLabel = SORT_OPTIONS.find((o) => o.value === search.sort)?.label
    if (sortLabel)
      activeFilterBadges.push({ label: `Sort: ${sortLabel}`, key: 'sort' })
  }

  function removeFilter(key: string) {
    if (key === 'q') setQInput('')
    if (key === 'publishedFrom') setPublishedFrom('')
    if (key === 'publishedTo') setPublishedTo('')
    updateFilter({ [key]: undefined })
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-3 flex-wrap">
        <Input
          type="search"
          placeholder="Search news…"
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
          value={search.source ?? ALL_VALUE}
          onValueChange={(v) =>
            updateFilter({
              source: v === ALL_VALUE ? undefined : (v as NewsSource),
            })
          }
        >
          <SelectTrigger size="sm">
            <SelectValue placeholder="Source" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_VALUE}>All sources</SelectItem>
            {SOURCE_OPTIONS.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={search.feedName ?? ALL_VALUE}
          onValueChange={(v) =>
            updateFilter({ feedName: v === ALL_VALUE ? undefined : v })
          }
        >
          <SelectTrigger size="sm">
            <SelectValue placeholder="Feed" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_VALUE}>All feeds</SelectItem>
            {FEED_OPTIONS.map((feed) => (
              <SelectItem key={feed} value={feed}>
                {feed}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <div className="flex items-center gap-1">
          <Input
            type="date"
            value={publishedFrom}
            onChange={(e) => setPublishedFrom(e.target.value)}
            onBlur={() => applyDate('publishedFrom', publishedFrom)}
            className="h-8 w-36 text-sm"
            aria-label="Published from"
          />
          <span className="text-xs">–</span>
          <Input
            type="date"
            value={publishedTo}
            onChange={(e) => setPublishedTo(e.target.value)}
            onBlur={() => applyDate('publishedTo', publishedTo)}
            className="h-8 w-36 text-sm"
            aria-label="Published to"
          />
        </div>

        <Select
          value={search.sort ?? 'publishedAt,desc'}
          onValueChange={(v) =>
            updateFilter({
              sort:
                v === 'publishedAt,desc' ? undefined : (v as NewsSortOption),
            })
          }
        >
          <SelectTrigger size="sm">
            <SelectValue placeholder="Sort by" />
          </SelectTrigger>
          <SelectContent>
            {SORT_OPTIONS.map((option) => (
              <SelectItem
                key={option.value}
                value={option.value}
                disabled={option.value === 'relevance' && !hasQuery}
              >
                {option.label}
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
