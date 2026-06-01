import { Link } from '@tanstack/react-router'
import { AuroraText } from '@/components/magicui/aurora-text'
import { Particles } from '@/components/magicui/particles'
import { RainbowButton } from '@/components/magicui/rainbow-button'
import { Button } from '@/components/ui/button'

// Violet tones derived from the site's primary so the gradient stays on-brand.
const AURORA_COLORS = ['#a78bfa', '#8b5cf6', '#7c3aed', '#a78bfa']

/**
 * Marketing hero for logged-out visitors: a subtle particle backdrop, an
 * aurora-gradient headline tinted with the site's violet, and the register /
 * sign-in actions.
 */
export function HeroSection() {
  return (
    <section className="relative overflow-hidden">
      <Particles
        className="absolute inset-0"
        quantity={120}
        ease={80}
        color="#a78bfa"
        refresh
      />
      <div className="relative mx-auto max-w-4xl px-4 py-24 text-center sm:py-32">
        <h1 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
          Your gaming journey,{' '}
          <AuroraText colors={AURORA_COLORS}>all in one place</AuroraText>
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-lg text-muted-foreground sm:text-xl">
          Track your game library, rate and review titles, create curated lists,
          and share your gaming experiences with friends.
        </p>
        <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
          <RainbowButton asChild size="lg">
            <Link to="/register">Join CheckPoint</Link>
          </RainbowButton>
          <Button asChild variant="outline" size="lg">
            <Link to="/login">Sign in</Link>
          </Button>
        </div>
      </div>
    </section>
  )
}
