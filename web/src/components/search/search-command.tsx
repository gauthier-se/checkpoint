import { useDeferredValue, useEffect, useRef, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { Gamepad2, Loader2, Monitor, Newspaper, Tag, Users } from 'lucide-react'
import {
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  genresQueryOptions,
  platformsQueryOptions,
  searchGamesQueryOptions,
} from '@/queries/catalog'
import { searchMembersQueryOptions } from '@/queries/members'
import { searchNewsQueryOptions } from '@/queries/news'
import { triggerRickroll } from '@/queries/easter-eggs'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import { resolvePictureUrl } from '@/lib/picture'

type SearchTab = 'all' | 'games' | 'members' | 'news' | 'genres' | 'platforms'

interface SearchCommandProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

// Patterns that flag a Rickroll attempt anywhere in the global search.
const RICKROLL_PATTERNS = [
  /rickroll/i,
  /never gonna give you up/i,
  /dQw4w9WgXcQ/, // canonical Rickroll YouTube ID
] as const

export function SearchCommand({ open, onOpenChange }: SearchCommandProps) {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [tab, setTab] = useState<SearchTab>('all')
  const deferredQuery = useDeferredValue(query)
  const isSearchActive = deferredQuery.length >= 2

  // RICKROLL: fire-and-forget when the user types one of the giveaway phrases.
  // The local flag means we only POST once per session, but the badge is
  // server-side idempotent either way.
  const rickrolledRef = useRef(false)
  useEffect(() => {
    if (rickrolledRef.current) return
    if (RICKROLL_PATTERNS.some((re) => re.test(deferredQuery))) {
      rickrolledRef.current = true
      void triggerRickroll()
    }
  }, [deferredQuery])

  const showGames = tab === 'all' || tab === 'games'
  const showMembers = tab === 'all' || tab === 'members'
  const showNews = tab === 'all' || tab === 'news'
  const showGenres = tab === 'all' || tab === 'genres'
  const showPlatforms = tab === 'all' || tab === 'platforms'

  const {
    data: games,
    isLoading: isLoadingGames,
    isFetching: isFetchingGames,
  } = useQuery({
    ...searchGamesQueryOptions(deferredQuery),
    enabled: isSearchActive && showGames,
  })

  const {
    data: membersResponse,
    isLoading: isLoadingMembers,
    isFetching: isFetchingMembers,
  } = useQuery({
    ...searchMembersQueryOptions(deferredQuery),
    enabled: isSearchActive && showMembers,
  })

  const {
    data: news,
    isLoading: isLoadingNews,
    isFetching: isFetchingNews,
  } = useQuery({
    ...searchNewsQueryOptions(deferredQuery),
    enabled: isSearchActive && showNews,
  })

  const { data: genres } = useQuery(genresQueryOptions())
  const { data: platforms } = useQuery(platformsQueryOptions())

  const members = membersResponse?.content

  const filteredGenres =
    isSearchActive && showGenres
      ? genres?.filter((g) =>
          g.name.toLowerCase().includes(deferredQuery.toLowerCase()),
        )
      : undefined

  const filteredPlatforms =
    isSearchActive && showPlatforms
      ? platforms?.filter((p) =>
          p.name.toLowerCase().includes(deferredQuery.toLowerCase()),
        )
      : undefined

  const isLoading =
    isSearchActive &&
    ((showGames && isLoadingGames) ||
      (showMembers && isLoadingMembers) ||
      (showNews && isLoadingNews))

  const isFetching =
    isSearchActive &&
    ((showGames && isFetchingGames) ||
      (showMembers && isFetchingMembers) ||
      (showNews && isFetchingNews))

  const hasResults =
    (showGames && games && games.length > 0) ||
    (showMembers && members && members.length > 0) ||
    (showNews && news && news.length > 0) ||
    (showGenres && filteredGenres && filteredGenres.length > 0) ||
    (showPlatforms && filteredPlatforms && filteredPlatforms.length > 0)

  // Reset query and tab when dialog closes
  useEffect(() => {
    if (!open) {
      setQuery('')
      setTab('all')
    }
  }, [open])

  function handleSelect(callback: () => void) {
    onOpenChange(false)
    callback()
  }

  const gamesResults = showGames && games && games.length > 0 && (
    <CommandGroup heading="Games">
      {games.slice(0, 5).map((game) => (
        <CommandItem
          key={game.id}
          value={`game-${game.title}`}
          onSelect={() =>
            handleSelect(() =>
              navigate({
                to: '/games/$gameId',
                params: { gameId: game.id },
              }),
            )
          }
        >
          <Gamepad2 />
          <div className="flex items-center gap-3">
            {game.coverUrl && (
              <img
                src={game.coverUrl}
                alt=""
                className="h-8 w-6 rounded-sm object-cover"
              />
            )}
            <div>
              <p className="font-medium">{game.title}</p>
              {game.releaseDate && (
                <p className="text-xs text-muted-foreground">
                  {new Date(game.releaseDate).getFullYear()}
                </p>
              )}
            </div>
          </div>
        </CommandItem>
      ))}
    </CommandGroup>
  )

  const membersResults = showMembers && members && members.length > 0 && (
    <CommandGroup heading="Members">
      {members.map((member) => (
        <CommandItem
          key={member.id}
          value={`member-${member.pseudo}`}
          onSelect={() =>
            handleSelect(() =>
              navigate({
                to: '/profile/$username',
                params: { username: member.pseudo },
              }),
            )
          }
        >
          <Users />
          <div className="flex items-center gap-3">
            {member.picture && (
              <img
                src={resolvePictureUrl(member.picture)}
                alt=""
                className="size-6 rounded-full object-cover"
              />
            )}
            <div>
              <p className="font-medium">{member.pseudo}</p>
              <p className="text-xs text-muted-foreground">
                Level {member.level}
              </p>
            </div>
          </div>
        </CommandItem>
      ))}
    </CommandGroup>
  )

  const newsResults = showNews && news && news.length > 0 && (
    <CommandGroup heading="News">
      {news.slice(0, 5).map((article) => (
        <CommandItem
          key={article.id}
          value={`news-${article.title}`}
          onSelect={() =>
            handleSelect(() =>
              navigate({
                to: '/news/$newsId',
                params: { newsId: article.id },
              }),
            )
          }
        >
          <Newspaper />
          <div className="flex items-center gap-3">
            {article.picture && (
              <img
                src={article.picture}
                alt=""
                className="h-8 w-12 rounded-sm object-cover"
              />
            )}
            <div className="flex flex-col gap-0.5">
              <p className="font-medium line-clamp-1">{article.title}</p>
              <div className="flex items-center gap-2">
                <Badge variant="outline" className="text-[10px] py-0">
                  {article.source}
                </Badge>
                {article.feedName && (
                  <span className="text-xs text-muted-foreground">
                    {article.feedName}
                  </span>
                )}
              </div>
            </div>
          </div>
        </CommandItem>
      ))}
    </CommandGroup>
  )

  const genresResults = showGenres &&
    filteredGenres &&
    filteredGenres.length > 0 && (
      <CommandGroup heading="Genres">
        {filteredGenres.slice(0, 5).map((genre) => (
          <CommandItem
            key={genre.id}
            value={`genre-${genre.name}`}
            onSelect={() =>
              handleSelect(() =>
                navigate({
                  to: '/games/filtered',
                  search: { page: 1, genres: [genre.name] },
                }),
              )
            }
          >
            <Tag />
            <span>{genre.name}</span>
            {genre.videoGamesCount !== undefined && (
              <span className="ml-auto text-xs text-muted-foreground">
                {genre.videoGamesCount} games
              </span>
            )}
          </CommandItem>
        ))}
      </CommandGroup>
    )

  const platformsResults = showPlatforms &&
    filteredPlatforms &&
    filteredPlatforms.length > 0 && (
      <CommandGroup heading="Platforms">
        {filteredPlatforms.slice(0, 5).map((platform) => (
          <CommandItem
            key={platform.id}
            value={`platform-${platform.name}`}
            onSelect={() =>
              handleSelect(() =>
                navigate({
                  to: '/games/filtered',
                  search: { page: 1, platforms: [platform.name] },
                }),
              )
            }
          >
            <Monitor />
            <span>{platform.name}</span>
            {platform.videoGamesCount !== undefined && (
              <span className="ml-auto text-xs text-muted-foreground">
                {platform.videoGamesCount} games
              </span>
            )}
          </CommandItem>
        ))}
      </CommandGroup>
    )

  return (
    <CommandDialog
      open={open}
      onOpenChange={onOpenChange}
      title="Search"
      description="Search for games, members, news, genres, and platforms"
      showCloseButton={false}
    >
      <CommandInput
        placeholder="Search games, members, news…"
        value={query}
        onValueChange={setQuery}
      />
      <Tabs
        value={tab}
        onValueChange={(value) => setTab(value as SearchTab)}
        className="gap-0"
      >
        <TabsList className="w-full justify-start rounded-none border-b px-2">
          <TabsTrigger value="all">All</TabsTrigger>
          <TabsTrigger value="games">
            <Gamepad2 className="size-3.5" />
            Games
          </TabsTrigger>
          <TabsTrigger value="members">
            <Users className="size-3.5" />
            Members
          </TabsTrigger>
          <TabsTrigger value="news">
            <Newspaper className="size-3.5" />
            News
          </TabsTrigger>
          <TabsTrigger value="genres">
            <Tag className="size-3.5" />
            Genres
          </TabsTrigger>
          <TabsTrigger value="platforms">
            <Monitor className="size-3.5" />
            Platforms
          </TabsTrigger>
        </TabsList>
        <CommandList className="relative">
          {isFetching && !isLoading && (
            <div className="absolute right-4 top-4 z-10">
              <Loader2 className="size-4 animate-spin text-muted-foreground opacity-50" />
            </div>
          )}
          {isLoading && (
            <div className="flex items-center justify-center py-6">
              <Loader2 className="size-5 animate-spin text-muted-foreground" />
            </div>
          )}

          {!isLoading && isSearchActive && !hasResults && (
            <CommandEmpty>No results found.</CommandEmpty>
          )}

          {!isSearchActive && (
            <div className="py-6 text-center text-sm text-muted-foreground">
              Type at least 2 characters to search...
            </div>
          )}

          <div
            className={cn(
              'transition-opacity duration-200',
              isFetching && !isLoading ? 'opacity-50' : 'opacity-100',
            )}
          >
            <TabsContent value="all" className="mt-0">
              {gamesResults}
              {newsResults}
              {membersResults}
              {genresResults}
              {platformsResults}
            </TabsContent>
            <TabsContent value="games" className="mt-0">
              {gamesResults}
            </TabsContent>
            <TabsContent value="members" className="mt-0">
              {membersResults}
            </TabsContent>
            <TabsContent value="news" className="mt-0">
              {newsResults}
            </TabsContent>
            <TabsContent value="genres" className="mt-0">
              {genresResults}
            </TabsContent>
            <TabsContent value="platforms" className="mt-0">
              {platformsResults}
            </TabsContent>
          </div>
        </CommandList>
      </Tabs>
    </CommandDialog>
  )
}
