import { useQuery } from '@tanstack/react-query'
import type { Game } from '@/types/game'
import type { NewsArticle } from '@/types/news'
import type { User } from '@/types/user'
import { RecommendedForYouSection } from '@/components/games/recommended-for-you-section'
import { OnboardingChecklist } from '@/components/onboarding/onboarding-checklist'
import { WelcomeSection } from '@/components/home/welcome-section'
import { FriendsActivitySection } from '@/components/home/friends-activity-section'
import { FriendsTrendingSection } from '@/components/home/friends-trending-section'
import { TrendingSection } from '@/components/home/trending-section'
import { SuggestedMembersSection } from '@/components/home/suggested-members-section'
import { PopularListsSection } from '@/components/home/popular-lists-section'
import { NewsSection } from '@/components/home/news-section'
import { recommendedGamesQueryOptions } from '@/queries/catalog'
import { userProfileQueryOptions } from '@/queries/profile'
import { suggestedMembersQueryOptions } from '@/queries/members'
import { popularListsQueryOptions } from '@/queries/lists'
import { friendsTrendingGamesQueryOptions } from '@/queries/feed'

interface AuthenticatedHomeProps {
  user: User
  trending: Array<Game>
  news: Array<NewsArticle>
}

export function AuthenticatedHome({
  user,
  trending,
  news,
}: AuthenticatedHomeProps) {
  const profileQuery = useQuery(userProfileQueryOptions(user.username))
  const suggestedQuery = useQuery(suggestedMembersQueryOptions(5))
  const popularListsQuery = useQuery(popularListsQueryOptions(0, 4))
  const friendsTrendingQuery = useQuery(friendsTrendingGamesQueryOptions(7))
  const recommendedQuery = useQuery(recommendedGamesQueryOptions(7))

  return (
    <div className="mx-auto max-w-7xl px-4">
      <OnboardingChecklist />
      <div className="flex flex-col gap-6 py-8">
        <WelcomeSection user={user} profile={profileQuery.data} />
        <FriendsActivitySection />
      </div>
      <FriendsTrendingSection
        games={friendsTrendingQuery.data}
        isLoading={friendsTrendingQuery.isLoading}
      />
      <RecommendedForYouSection
        games={recommendedQuery.data}
        isLoading={recommendedQuery.isLoading}
      />
      <TrendingSection games={trending} />
      <SuggestedMembersSection
        members={suggestedQuery.data}
        isLoading={suggestedQuery.isLoading}
      />
      <PopularListsSection
        lists={popularListsQuery.data?.content}
        isLoading={popularListsQuery.isLoading}
      />
      <NewsSection articles={news} />
    </div>
  )
}
