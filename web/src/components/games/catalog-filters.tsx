import { useEffect, useState } from 'react'
import { useNavigate, useRouterState } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { X } from 'lucide-react'
import type { KeyboardEvent } from 'react'
import { genresQueryOptions, platformsQueryOptions } from '@/queries/catalog'
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

type CatalogFiltersSearch = {
  page?: number
  q?: string
  genre?: string
  platform?: string
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

const ALL_VALUE = '__all__'

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

  const hasActiveFilters =
    search.genre != null ||
    search.platform != null ||
    search.yearMin != null ||
    search.yearMax != null ||
    search.ratingMin != null ||
    search.ratingMax != null ||
    search.sort != null

  function clearAllFilters() {
    setYearMin('')
    setYearMax('')
    setRatingMin('')
    setRatingMax('')
    navigate({ to: '/games', search: { page: 1 } })
  }

  const activeFilterBadges: Array<{ label: string; key: string }> = []
  if (search.genre)
    activeFilterBadges.push({ label: `Genre: ${search.genre}`, key: 'genre' })
  if (search.platform)
    activeFilterBadges.push({
      label: `Platform: ${search.platform}`,
      key: 'platform',
    })
  if (search.yearMin != null)
    activeFilterBadges.push({
      label: `Year from: ${search.yearMin}`,
      key: 'yearMin',
    })
  if (search.yearMax != null)
    activeFilterBadges.push({
      label: `Year to: ${search.yearMax}`,
      key: 'yearMax',
    })
  if (search.ratingMin != null)
    activeFilterBadges.push({
      label: `Rating min: ${search.ratingMin}`,
      key: 'ratingMin',
    })
  if (search.ratingMax != null)
    activeFilterBadges.push({
      label: `Rating max: ${search.ratingMax}`,
      key: 'ratingMax',
    })
  if (search.sort) {
    const sortLabel = SORT_OPTIONS.find((o) => o.value === search.sort)?.label
    if (sortLabel)
      activeFilterBadges.push({ label: `Sort: ${sortLabel}`, key: 'sort' })
  }

  function removeFilter(key: string) {
    if (key === 'yearMin') setYearMin('')
    if (key === 'yearMax') setYearMax('')
    if (key === 'ratingMin') setRatingMin('')
    if (key === 'ratingMax') setRatingMax('')
    updateFilter({ [key]: undefined })
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-3 flex-wrap">
        <p>Browse by</p>

        <Select
          value={search.genre ?? ALL_VALUE}
          onValueChange={(v) =>
            updateFilter({ genre: v === ALL_VALUE ? undefined : v })
          }
        >
          <SelectTrigger size="sm">
            <SelectValue placeholder="Genre" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_VALUE}>All genres</SelectItem>
            {genres?.map((genre) => (
              <SelectItem key={genre.id} value={genre.name}>
                {genre.name}
                {genre.videoGamesCount != null && (
                  <span className="text-muted-foreground ml-1">
                    ({genre.videoGamesCount})
                  </span>
                )}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={search.platform ?? ALL_VALUE}
          onValueChange={(v) =>
            updateFilter({ platform: v === ALL_VALUE ? undefined : v })
          }
        >
          <SelectTrigger size="sm">
            <SelectValue placeholder="Platform" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_VALUE}>All platforms</SelectItem>
            {platforms?.map((platform) => (
              <SelectItem key={platform.id} value={platform.name}>
                {platform.name}
                {platform.videoGamesCount != null && (
                  <span className="text-muted-foreground ml-1">
                    ({platform.videoGamesCount})
                  </span>
                )}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <div className="flex items-center gap-1">
          <Input
            type="number"
            placeholder="Year from"
            value={yearMin}
            onChange={(e) => setYearMin(e.target.value)}
            onBlur={() => applyNumberFilter('yearMin', yearMin, search.yearMin)}
            onKeyDown={(e) =>
              handleNumberKeyDown(e, 'yearMin', yearMin, search.yearMin)
            }
            className="h-8 w-24 text-sm"
          />
          <span className="text-xs">-</span>
          <Input
            type="number"
            placeholder="Year to"
            value={yearMax}
            onChange={(e) => setYearMax(e.target.value)}
            onBlur={() => applyNumberFilter('yearMax', yearMax, search.yearMax)}
            onKeyDown={(e) =>
              handleNumberKeyDown(e, 'yearMax', yearMax, search.yearMax)
            }
            className="h-8 w-24 text-sm"
          />
        </div>

        <div className="flex items-center gap-1">
          <Input
            type="number"
            placeholder="Rating min"
            value={ratingMin}
            onChange={(e) => setRatingMin(e.target.value)}
            onBlur={() =>
              applyNumberFilter('ratingMin', ratingMin, search.ratingMin)
            }
            onKeyDown={(e) =>
              handleNumberKeyDown(e, 'ratingMin', ratingMin, search.ratingMin)
            }
            step={0.5}
            min={1}
            max={5}
            className="h-8 w-26 text-sm"
          />
          <span className="text-xs">-</span>
          <Input
            type="number"
            placeholder="Rating max"
            value={ratingMax}
            onChange={(e) => setRatingMax(e.target.value)}
            onBlur={() =>
              applyNumberFilter('ratingMax', ratingMax, search.ratingMax)
            }
            onKeyDown={(e) =>
              handleNumberKeyDown(e, 'ratingMax', ratingMax, search.ratingMax)
            }
            step={0.5}
            min={1}
            max={5}
            className="h-8 w-26 text-sm"
          />
        </div>

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
