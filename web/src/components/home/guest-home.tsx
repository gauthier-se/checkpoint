import type { Game } from '@/types/game'
import type { NewsArticle } from '@/types/news'
import { HeroSection } from '@/components/home/hero-section'
import { TrendingSection } from '@/components/home/trending-section'
import { FeaturesSection } from '@/components/home/features-section'
import { NewsSection } from '@/components/home/news-section'
import { CtaSection } from '@/components/home/cta-section'

interface GuestHomeProps {
  trending: Array<Game>
  news: Array<NewsArticle>
}

export function GuestHome({ trending, news }: GuestHomeProps) {
  return (
    <div>
      <HeroSection />
      <div className="mx-auto max-w-7xl px-4">
        <TrendingSection games={trending} />
        <FeaturesSection />
        <NewsSection articles={news} />
      </div>
      <CtaSection />
    </div>
  )
}
