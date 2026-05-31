import { useEffect, useState } from 'react'
import { useNavigate, useRouterState } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { SlidersHorizontal, X } from 'lucide-react'
import type { KeyboardEvent } from 'react'
import { genresQueryOptions, platformsQueryOptions } from '@/queries/catalog'
import { MultiSelectFilter } from '@/components/games/multi-select-filter'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

type CatalogFiltersSearch = {
  page?: number
  q?: string
  genres?: Array<string>
  platforms?: Array<string>
  yearMin?: number
  yearMax?: number
  ratingMin?: number
  ratingMax?: number
  sort?: string
}

const SORT_OPTIONS = [
  { value: 'releaseDate,desc', label: 'Release Date (newest)' },
  { value: 'releaseDate,asc', label: 'Release Date (oldest)' },
  { value: 'title,asc', label: 'Title (A-Z)' },
  { value: 'title,desc', label: 'Title (Z-A)' },
  { value: 'rating,desc', label: 'Rating (highest)' },
  { value: 'rating,asc', label: 'Rating (lowest)' },
] as const

interface CatalogFiltersProps {
  search: CatalogFiltersSearch
}

export function CatalogFilters({ search }: CatalogFiltersProps) {
  const navigate = useNavigate()
  const pathname = useRouterState({ select: (s) => s.location.pathname })
  const isOnFiltered = pathname.startsWith('/games/filtered')

  const { data: genres } = useQuery(genresQueryOptions())
  const { data: platforms } = useQuery(platformsQueryOptions())

  const [yearMin, setYearMin] = useState(search.yearMin?.toString() ?? '')
  const [yearMax, setYearMax] = useState(search.yearMax?.toString() ?? '')
  const [ratingMin, setRatingMin] = useState(search.ratingMin?.toString() ?? '')
  const [ratingMax, setRatingMax] = useState(search.ratingMax?.toString() ?? '')

  // Sync local state when URL params change (e.g. clear all, browser back)
  useEffect(() => {
    setYearMin(search.yearMin?.toString() ?? '')
    setYearMax(search.yearMax?.toString() ?? '')
    setRatingMin(search.ratingMin?.toString() ?? '')
    setRatingMax(search.ratingMax?.toString() ?? '')
  }, [search.yearMin, search.yearMax, search.ratingMin, search.ratingMax])

  function updateFilter(updates: Record<string, unknown>) {
    if (isOnFiltered) {
      navigate({
        to: '/games/filtered',
        search: (prev) => ({ ...prev, ...updates, page: 1 }),
      })
    } else {
      navigate({
        to: '/games/filtered',
        search: { ...search, ...updates, page: 1 },
      })
    }
  }

  function applyNumberFilter(
    key: string,
    value: string,
    currentUrlValue: number | undefined,
  ) {
    const trimmed = value.trim()
    if (trimmed === '') {
      if (currentUrlValue != null) {
        updateFilter({ [key]: undefined })
      }
      return
    }
    const n = Number(trimmed)
    if (Number.isFinite(n) && n !== currentUrlValue) {
      updateFilter({ [key]: n })
    }
  }

  function handleNumberKeyDown(
    e: KeyboardEvent<HTMLInputElement>,
    key: string,
    value: string,
    currentUrlValue: number | undefined,
  ) {
    if (e.key === 'Enter') {
      applyNumberFilter(key, value, currentUrlValue)
    }
  }

  const selectedGenres = search.genres ?? []
  const selectedPlatforms = search.platforms ?? []

  // Count of advanced (year / rating) filters surfaced in the "More filters" panel.
  const advancedFilterCount =
    (search.yearMin != null ? 1 : 0) +
    (search.yearMax != null ? 1 : 0) +
    (search.ratingMin != null ? 1 : 0) +
    (search.ratingMax != null ? 1 : 0)

  const hasActiveFilters =
    selectedGenres.length > 0 ||
    selectedPlatforms.length > 0 ||
    advancedFilterCount > 0 ||
    search.sort != null

  function clearAllFilters() {
    setYearMin('')
    setYearMax('')
    setRatingMin('')
    setRatingMax('')
    navigate({ to: '/games', search: { page: 1 } })
  }

  const activeFilterBadges: Array<{ label: string; onRemove: () => void }> = []
  for (const genre of selectedGenres) {
    activeFilterBadges.push({
      label: `Genre: ${genre}`,
      onRemove: () => {
        const next = selectedGenres.filter((g) => g !== genre)
        updateFilter({ genres: next.length > 0 ? next : undefined })
      },
    })
  }
  for (const platform of selectedPlatforms) {
    activeFilterBadges.push({
      label: `Platform: ${platform}`,
      onRemove: () => {
        const next = selectedPlatforms.filter((p) => p !== platform)
        updateFilter({ platforms: next.length > 0 ? next : undefined })
      },
    })
  }
  if (search.yearMin != null)
    activeFilterBadges.push({
      label: `Year from: ${search.yearMin}`,
      onRemove: () => {
        setYearMin('')
        updateFilter({ yearMin: undefined })
      },
    })
  if (search.yearMax != null)
    activeFilterBadges.push({
      label: `Year to: ${search.yearMax}`,
      onRemove: () => {
        setYearMax('')
        updateFilter({ yearMax: undefined })
      },
    })
  if (search.ratingMin != null)
    activeFilterBadges.push({
      label: `Rating min: ${search.ratingMin}`,
      onRemove: () => {
        setRatingMin('')
        updateFilter({ ratingMin: undefined })
      },
    })
  if (search.ratingMax != null)
    activeFilterBadges.push({
      label: `Rating max: ${search.ratingMax}`,
      onRemove: () => {
        setRatingMax('')
        updateFilter({ ratingMax: undefined })
      },
    })
  if (search.sort) {
    const sortLabel = SORT_OPTIONS.find((o) => o.value === search.sort)?.label
    if (sortLabel)
      activeFilterBadges.push({
        label: `Sort: ${sortLabel}`,
        onRemove: () => updateFilter({ sort: undefined }),
      })
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-2 flex-wrap xl:flex-nowrap">
        <p className="mr-1">Browse by</p>

        <MultiSelectFilter
          label="Genre"
          options={
            genres?.map((g) => ({
              value: g.name,
              label: g.name,
              count: g.videoGamesCount,
            })) ?? []
          }
          selected={selectedGenres}
          onChange={(next) =>
            updateFilter({ genres: next.length > 0 ? next : undefined })
          }
        />

        <MultiSelectFilter
          label="Platform"
          options={
            platforms?.map((p) => ({
              value: p.name,
              label: p.name,
              count: p.videoGamesCount,
            })) ?? []
          }
          selected={selectedPlatforms}
          onChange={(next) =>
            updateFilter({ platforms: next.length > 0 ? next : undefined })
          }
        />

        <Popover>
          <PopoverTrigger asChild>
            <Button
              variant="outline"
              size="sm"
              className="h-8 gap-1.5 font-normal"
            >
              <SlidersHorizontal className="size-3.5" />
              More filters
              {advancedFilterCount > 0 && (
                <span className="text-muted-foreground">
                  ({advancedFilterCount})
                </span>
              )}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-72 space-y-4" align="start">
            <div className="space-y-2">
              <Label className="text-xs font-semibold">Release year</Label>
              <div className="flex items-center gap-2">
                <Input
                  type="number"
                  placeholder="From"
                  value={yearMin}
                  onChange={(e) => setYearMin(e.target.value)}
                  onBlur={() =>
                    applyNumberFilter('yearMin', yearMin, search.yearMin)
                  }
                  onKeyDown={(e) =>
                    handleNumberKeyDown(e, 'yearMin', yearMin, search.yearMin)
                  }
                  className="h-8 text-sm"
                />
                <span className="text-xs text-muted-foreground">to</span>
                <Input
                  type="number"
                  placeholder="To"
                  value={yearMax}
                  onChange={(e) => setYearMax(e.target.value)}
                  onBlur={() =>
                    applyNumberFilter('yearMax', yearMax, search.yearMax)
                  }
                  onKeyDown={(e) =>
                    handleNumberKeyDown(e, 'yearMax', yearMax, search.yearMax)
                  }
                  className="h-8 text-sm"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label className="text-xs font-semibold">Rating</Label>
              <div className="flex items-center gap-2">
                <Input
                  type="number"
                  placeholder="Min"
                  value={ratingMin}
                  onChange={(e) => setRatingMin(e.target.value)}
                  onBlur={() =>
                    applyNumberFilter('ratingMin', ratingMin, search.ratingMin)
                  }
                  onKeyDown={(e) =>
                    handleNumberKeyDown(
                      e,
                      'ratingMin',
                      ratingMin,
                      search.ratingMin,
                    )
                  }
                  step={0.5}
                  min={1}
                  max={5}
                  className="h-8 text-sm"
                />
                <span className="text-xs text-muted-foreground">to</span>
                <Input
                  type="number"
                  placeholder="Max"
                  value={ratingMax}
                  onChange={(e) => setRatingMax(e.target.value)}
                  onBlur={() =>
                    applyNumberFilter('ratingMax', ratingMax, search.ratingMax)
                  }
                  onKeyDown={(e) =>
                    handleNumberKeyDown(
                      e,
                      'ratingMax',
                      ratingMax,
                      search.ratingMax,
                    )
                  }
                  step={0.5}
                  min={1}
                  max={5}
                  className="h-8 text-sm"
                />
              </div>
            </div>
          </PopoverContent>
        </Popover>

        <Select
          value={search.sort ?? 'releaseDate,desc'}
          onValueChange={(v) =>
            updateFilter({
              sort: v === 'releaseDate,desc' ? undefined : v,
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
      </div>

      {hasActiveFilters && (
        <div className="flex items-center gap-2 flex-wrap">
          {activeFilterBadges.map((badge) => (
            <Badge key={badge.label} variant="secondary" className="gap-1">
              {badge.label}
              <button
                type="button"
                onClick={badge.onRemove}
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
