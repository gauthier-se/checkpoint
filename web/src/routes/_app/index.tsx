import { Link, createFileRoute } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import {
  Gamepad2,
  Heart,
  ListChecks,
  MessageSquare,
  Star,
  Users,
  Users2,
} from 'lucide-react'
import type { Game } from '@/types/game'
import type { NewsArticle } from '@/types/news'
import type { User } from '@/types/user'
import type { UserProfile } from '@/types/profile'
import type { MemberCard as MemberCardType } from '@/types/member'
import type { GameListCard } from '@/types/list'
import type { FeedTab } from '@/types/feed'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { GameGrid } from '@/components/games/game-grid'
import { RecommendedForYouSection } from '@/components/games/recommended-for-you-section'
import { NewsCard } from '@/components/news/news-card'
import { MemberCard } from '@/components/members/member-card'
import { ListsGrid } from '@/components/lists/lists-grid'
import { FeedList, FeedListSkeleton } from '@/components/feed/feed-list'
import { FeedTabs } from '@/components/feed/feed-tabs'
import { OnboardingChecklist } from '@/components/onboarding/onboarding-checklist'
import { useAuth } from '@/hooks/use-auth'
import {
  recommendedGamesQueryOptions,
  trendingGamesQueryOptions,
} from '@/queries/catalog'
import { newsListQueryOptions } from '@/queries/news'
import { userProfileQueryOptions } from '@/queries/profile'
import { suggestedMembersQueryOptions } from '@/queries/members'
import { popularListsQueryOptions } from '@/queries/lists'
import {
  feedQueryOptions,
  friendsTrendingGamesQueryOptions,
} from '@/queries/feed'

import { seo } from '@/lib/seo'

interface HomeData {
  trending: Array<Game>
  news: Array<NewsArticle>
}

export const Route = createFileRoute('/_app/')({
  head: () => ({
    meta: seo({
      title: 'Checkpoint — Your gaming journal',
      description:
        'Track the games you play, rate and review them, build lists, and follow what your friends are playing.',
    }),
  }),
  component: App,
  loader: async ({ context }): Promise<HomeData> => {
    const [trending, newsResponse] = await Promise.all([
      context.queryClient.ensureQueryData(trendingGamesQueryOptions()),
      context.queryClient.ensureQueryData(newsListQueryOptions(0, 3)),
    ])
    return { trending, news: newsResponse.content }
  },
})

function App() {
  const { user, isLoading } = useAuth()
  const data = Route.useLoaderData()

  if (isLoading) {
    return <div className="min-h-[60vh]" />
  }

  if (user) {
    return <AuthenticatedHome user={user} data={data} />
  }

  return (
    <div>
      <HeroSection />
      <div className="mx-auto max-w-7xl px-4">
        <TrendingSection games={data.trending} />
        <FeaturesSection />
        <NewsSection articles={data.news} />
      </div>
      <CtaSection />
    </div>
  )
}

function HeroSection() {
  return (
    <section className="py-24 sm:py-32">
      <div className="mx-auto max-w-4xl px-4 text-center">
        <h1 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
          Your gaming journey,{' '}
          <span className="text-primary">all in one place</span>
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-lg text-muted-foreground sm:text-xl">
          Track your game library, rate and review titles, create curated lists,
          and share your gaming experiences with friends.
        </p>
        <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
          <Button asChild size="lg">
            <Link to="/register">Join CheckPoint</Link>
          </Button>
          <Button asChild variant="outline" size="lg">
            <Link to="/login">Sign in</Link>
          </Button>
        </div>
      </div>
    </section>
  )
}

function TrendingSection({ games }: { games: Array<Game> }) {
  if (games.length === 0) return null

  return (
    <section className="my-12">
      <div className="flex items-center justify-between py-2">
        <h2 className="text-muted-foreground font-semibold">
          Popular games this week
        </h2>
        <Link
          to="/games"
          search={{ page: 1 }}
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          See all
        </Link>
      </div>
      <Separator />
      <GameGrid games={games} columns={7} />
    </section>
  )
}

const features = [
  {
    icon: Gamepad2,
    title: 'Track your library',
    description:
      'Keep a record of every game you own, are playing, or want to play.',
  },
  {
    icon: Star,
    title: 'Rate and review',
    description:
      'Share your opinions and help others discover their next favorite game.',
  },
  {
    icon: ListChecks,
    title: 'Create curated lists',
    description:
      'Organize games into themed collections and share them with the community.',
  },
  {
    icon: Users,
    title: 'Connect with friends',
    description:
      'Follow other players, see what they are playing, and discover new games.',
  },
] as const

function FeaturesSection() {
  return (
    <section className="my-12">
      <div className="py-2">
        <h2 className="text-muted-foreground font-semibold">
          Everything you need
        </h2>
      </div>
      <Separator />
      <div className="grid grid-cols-1 gap-6 py-8 sm:grid-cols-2 lg:grid-cols-4">
        {features.map((feature) => (
          <div
            key={feature.title}
            className="flex flex-col items-center gap-3 rounded-lg border p-6 text-center"
          >
            <div className="flex size-12 items-center justify-center rounded-full bg-primary/10">
              <feature.icon className="size-6 text-primary" />
            </div>
            <h3 className="font-semibold">{feature.title}</h3>
            <p className="text-sm text-muted-foreground">
              {feature.description}
            </p>
          </div>
        ))}
      </div>
    </section>
  )
}

function NewsSection({ articles }: { articles: Array<NewsArticle> }) {
  if (articles.length === 0) return null

  return (
    <section className="my-12">
      <div className="flex items-center justify-between py-2">
        <h2 className="text-muted-foreground font-semibold">Latest news</h2>
        <Link
          to="/news"
          search={{ page: 1 }}
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          See all
        </Link>
      </div>
      <Separator />
      <div className="grid grid-cols-1 gap-4 py-4 sm:grid-cols-2 lg:grid-cols-3">
        {articles.map((article) => (
          <NewsCard key={article.id} article={article} />
        ))}
      </div>
    </section>
  )
}

function CtaSection() {
  return (
    <section className="border-t py-16">
      <div className="mx-auto max-w-2xl px-4 text-center">
        <h2 className="text-2xl font-bold sm:text-3xl">Join the community</h2>
        <p className="mt-4 text-muted-foreground">
          Start tracking your games and connect with other players today.
        </p>
        <Button asChild size="lg" className="mt-8">
          <Link to="/register">Create your account</Link>
        </Button>
      </div>
    </section>
  )
}

function AuthenticatedHome({ user, data }: { user: User; data: HomeData }) {
  const profileQuery = useQuery(userProfileQueryOptions(user.username))
  const suggestedQuery = useQuery(suggestedMembersQueryOptions(5))
  const popularListsQuery = useQuery(popularListsQueryOptions(0, 4))
  const friendsTrendingQuery = useQuery(friendsTrendingGamesQueryOptions(7))
  const recommendedQuery = useQuery(recommendedGamesQueryOptions(7))

  return (
    <div className="mx-auto max-w-7xl px-4">
      <OnboardingChecklist />
      <WelcomeSection user={user} profile={profileQuery.data} />
      <FriendsActivitySection />
      <FriendsTrendingSection
        games={friendsTrendingQuery.data}
        isLoading={friendsTrendingQuery.isLoading}
      />
      <RecommendedForYouSection
        games={recommendedQuery.data}
        isLoading={recommendedQuery.isLoading}
      />
      <TrendingSection games={data.trending} />
      <SuggestedMembersSection
        members={suggestedQuery.data}
        isLoading={suggestedQuery.isLoading}
      />
      <PopularListsSection
        lists={popularListsQuery.data?.content}
        isLoading={popularListsQuery.isLoading}
      />
      <NewsSection articles={data.news} />
    </div>
  )
}

const stats = [
  { key: 'reviewCount', label: 'Reviews', icon: MessageSquare },
  { key: 'wishlistCount', label: 'Wishlist', icon: Heart },
  { key: 'followerCount', label: 'Followers', icon: Users2 },
  { key: 'followingCount', label: 'Following', icon: Users },
] as const

function WelcomeSection({
  user,
  profile,
}: {
  user: User
  profile: UserProfile | undefined
}) {
  const xpPercent = profile
    ? Math.round((profile.xpPoint / profile.xpThreshold) * 100)
    : 0

  return (
    <section className="py-10">
      <div className="mb-6">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
          Welcome back, {user.username}!
        </h1>
        {profile && (
          <div className="mt-2 flex items-center gap-3">
            <span className="text-sm font-medium text-muted-foreground">
              Level {profile.level}
            </span>
            <div className="h-2 w-32 overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-primary transition-all"
                style={{ width: `${xpPercent}%` }}
              />
            </div>
            <span className="text-xs text-muted-foreground">
              {profile.xpPoint}/{profile.xpThreshold} XP
            </span>
          </div>
        )}
      </div>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.key} className="py-4">
            <CardContent className="flex items-center gap-3">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary/10">
                <stat.icon className="size-5 text-primary" />
              </div>
              <div>
                <p className="text-2xl font-bold">
                  {profile ? profile[stat.key] : '--'}
                </p>
                <p className="text-sm text-muted-foreground">{stat.label}</p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </section>
  )
}

function SuggestedMembersSection({
  members,
  isLoading,
}: {
  members: Array<MemberCardType> | undefined
  isLoading: boolean
}) {
  if (isLoading || !members || members.length === 0) return null

  return (
    <section className="my-12">
      <div className="flex items-center justify-between py-2">
        <h2 className="font-semibold text-muted-foreground">
          People you might know
        </h2>
        <Link
          to="/members/all"
          search={{ page: 1 }}
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          See all
        </Link>
      </div>
      <Separator />
      <div className="grid grid-cols-2 gap-4 py-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
        {members.map((member) => (
          <MemberCard key={member.id} member={member} />
        ))}
      </div>
    </section>
  )
}

function PopularListsSection({
  lists,
  isLoading,
}: {
  lists: Array<GameListCard> | undefined
  isLoading: boolean
}) {
  if (isLoading || !lists || lists.length === 0) return null

  return (
    <section className="my-12">
      <div className="flex items-center justify-between py-2">
        <h2 className="font-semibold text-muted-foreground">Popular lists</h2>
        <Link
          to="/lists"
          search={{ page: 1 }}
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          See all
        </Link>
      </div>
      <Separator />
      <ListsGrid lists={lists} />
    </section>
  )
}

function FriendsActivitySection() {
  const [tab, setTab] = useState<FeedTab>('all')
  const feedQuery = useQuery(
    feedQueryOptions(0, 5, tab === 'all' ? undefined : tab),
  )
  const items = feedQuery.data?.content

  // First load (no data yet): show a skeleton rather than a blank gap.
  if (tab === 'all' && feedQuery.isLoading) {
    return (
      <section className="my-12">
        <div className="flex items-center justify-between py-2">
          <h2 className="font-semibold text-muted-foreground">
            New from friends
          </h2>
        </div>
        <Separator />
        <FeedListSkeleton />
      </section>
    )
  }

  // Loaded: hide the whole section if the user has no friend activity at all —
  // no point showing an empty teaser on the home page. Once the "All" tab has
  // returned items, the tabs stay mounted even if a filtered view is empty so
  // the user can switch back.
  if (tab === 'all' && (!items || items.length === 0)) {
    return null
  }

  return (
    <section className="my-12">
      <div className="flex items-center justify-between py-2">
        <h2 className="font-semibold text-muted-foreground">
          New from friends
        </h2>
        <Link
          to="/feed"
          search={{ page: 1, type: tab === 'all' ? undefined : tab }}
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          More
        </Link>
      </div>
      <Separator />
      <FeedTabs value={tab} onValueChange={setTab} className="mt-5 mb-1" />
      {feedQuery.isLoading ? (
        <FeedListSkeleton />
      ) : items && items.length > 0 ? (
        <FeedList items={items} />
      ) : (
        <p className="py-6 text-center text-sm text-muted-foreground">
          No activity to show for this filter.
        </p>
      )}
    </section>
  )
}

function FriendsTrendingSection({
  games,
  isLoading,
}: {
  games: Array<Game> | undefined
  isLoading: boolean
}) {
  if (isLoading || !games || games.length === 0) return null

  return (
    <section className="my-12">
      <div className="flex items-center justify-between py-2">
        <h2 className="font-semibold text-muted-foreground">
          Popular with friends
        </h2>
        <Link
          to="/games/popular-with-friends"
          search={{ page: 1 }}
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          More
        </Link>
      </div>
      <Separator />
      <GameGrid games={games} columns={7} />
    </section>
  )
}
