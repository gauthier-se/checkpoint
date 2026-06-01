import { createFileRoute } from '@tanstack/react-router'
import type { Game } from '@/types/game'
import type { NewsArticle } from '@/types/news'
import { useAuth } from '@/hooks/use-auth'
import { trendingGamesQueryOptions } from '@/queries/catalog'
import { newsListQueryOptions } from '@/queries/news'
import { GuestHome } from '@/components/home/guest-home'
import { AuthenticatedHome } from '@/components/home/authenticated-home'
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
      context.queryClient.ensureQueryData(newsListQueryOptions({ page: 1 }, 3)),
    ])
    return { trending, news: newsResponse.content }
  },
})

function App() {
  const { user, isLoading } = useAuth()
  const { trending, news } = Route.useLoaderData()

  if (isLoading) {
    return <div className="min-h-[60vh]" />
  }

  if (user) {
    return <AuthenticatedHome user={user} trending={trending} news={news} />
  }

  return <GuestHome trending={trending} news={news} />
}
