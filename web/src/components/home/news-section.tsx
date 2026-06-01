import { Link } from '@tanstack/react-router'
import type { NewsArticle } from '@/types/news'
import { NewsCard } from '@/components/news/news-card'
import { SectionHeader } from '@/components/home/section-header'

export function NewsSection({ articles }: { articles: Array<NewsArticle> }) {
  if (articles.length === 0) return null

  return (
    <section className="my-12">
      <SectionHeader
        title="Latest news"
        action={
          <Link
            to="/news"
            search={{ page: 1 }}
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            See all
          </Link>
        }
      />
      <div className="grid grid-cols-1 gap-4 py-4 sm:grid-cols-2 lg:grid-cols-3">
        {articles.map((article) => (
          <NewsCard key={article.id} article={article} />
        ))}
      </div>
    </section>
  )
}
