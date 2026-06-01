import { Gamepad2, ListChecks, Star, Users } from 'lucide-react'
import { SectionHeader } from '@/components/home/section-header'

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

export function FeaturesSection() {
  return (
    <section className="my-12">
      <SectionHeader title="Everything you need" />
      <div className="grid grid-cols-1 gap-8 py-10 sm:grid-cols-2 lg:grid-cols-4">
        {features.map((feature) => (
          <div
            key={feature.title}
            className="flex flex-col items-center gap-3 text-center"
          >
            <div className="flex size-14 items-center justify-center rounded-full bg-primary/10">
              <feature.icon className="size-6 text-primary" />
            </div>
            <h3 className="text-lg font-semibold">{feature.title}</h3>
            <p className="text-sm text-muted-foreground">
              {feature.description}
            </p>
          </div>
        ))}
      </div>
    </section>
  )
}
