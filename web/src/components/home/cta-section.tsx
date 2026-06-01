import { Link } from '@tanstack/react-router'
import { AuroraText } from '@/components/magicui/aurora-text'
import { RainbowButton } from '@/components/magicui/rainbow-button'

// Violet tones derived from the site's primary so the gradient stays on-brand.
const AURORA_COLORS = ['#a78bfa', '#8b5cf6', '#7c3aed', '#a78bfa']

export function CtaSection() {
  return (
    <section className="mx-auto max-w-2xl px-4 py-20 text-center">
      <h2 className="text-3xl font-bold sm:text-4xl">
        Join the <AuroraText colors={AURORA_COLORS}>community</AuroraText>
      </h2>
      <p className="mx-auto mt-4 max-w-md text-muted-foreground">
        Start tracking your games and connect with other players today.
      </p>
      <RainbowButton asChild size="lg" className="mt-8">
        <Link to="/register">Create your account</Link>
      </RainbowButton>
    </section>
  )
}
