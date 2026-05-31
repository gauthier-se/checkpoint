import { createFileRoute } from '@tanstack/react-router'
import {
  Gamepad2,
  Github,
  ListChecks,
  Mail,
  MapPin,
  Star,
  Users,
} from 'lucide-react'

import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/about')({
  head: () => ({
    meta: seo({ title: 'About — Checkpoint' }),
  }),
  component: AboutPage,
})

const features = [
  {
    icon: Gamepad2,
    title: 'Track your library',
    description:
      'Keep a comprehensive record of every game you own, are currently playing, or plan to conquer next.',
  },
  {
    icon: Star,
    title: 'Rate & Review',
    description:
      'Share your honest opinions, write deep-dive reviews, and help the community discover amazing games.',
  },
  {
    icon: ListChecks,
    title: 'Curate your lists',
    description:
      'Organize games into perfectly themed collections and share your impeccable taste with the world.',
  },
  {
    icon: Users,
    title: 'Social connections',
    description:
      'Follow fellow players, stay updated on their gaming journey, and find your next co-op companion.',
  },
] as const

function AboutPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-16 sm:py-24 space-y-32">
      {/* About Section */}
      <section id="about" className="scroll-mt-28 space-y-12">
        <div className="text-center space-y-6">
          <h1 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
            About <span className="text-primary">Checkpoint</span>
          </h1>
          <p className="mx-auto max-w-2xl text-lg text-muted-foreground sm:text-xl leading-relaxed">
            Checkpoint is your personal gaming companion. A place where you can
            track, rate, review, and curate your ultimate digital library while
            connecting with a community of passionate players.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 mt-16">
          {features.map((feature) => (
            <div
              key={feature.title}
              className="flex flex-col gap-3 rounded-xl border p-8 bg-card text-card-foreground shadow-sm transition-all hover:shadow-md"
            >
              <div className="flex size-14 items-center justify-center rounded-full bg-primary/10">
                <feature.icon className="size-7 text-primary" />
              </div>
              <h3 className="font-semibold text-xl mt-2">{feature.title}</h3>
              <p className="text-muted-foreground leading-relaxed">
                {feature.description}
              </p>
            </div>
          ))}
        </div>

        <div className="rounded-2xl bg-muted/50 p-8 sm:p-14 text-center space-y-6 mt-20">
          <div className="flex justify-center mb-6">
            <div className="flex size-20 items-center justify-center rounded-full bg-primary/10">
              <MapPin className="size-10 text-primary" />
            </div>
          </div>
          <h3 className="text-2xl sm:text-3xl font-bold">
            Made in Strasbourg, France
          </h3>
          <p className="text-muted-foreground text-lg max-w-2xl mx-auto leading-relaxed">
            Checkpoint is lovingly crafted by a truly passionate and dedicated
            team nestled in the heart of Europe. We believe in building a
            platform that puts the player's experience first, keeping things
            focused on the pure joy of tracking games and sharing experiences.
          </p>
        </div>
      </section>

      {/* Divider */}
      <hr className="border-t border-border" />

      {/* Contact Section */}
      <section id="contact" className="scroll-mt-28 space-y-12">
        <div className="text-center space-y-6">
          <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
            Get in Touch
          </h2>
          <p className="mx-auto max-w-2xl text-lg text-muted-foreground leading-relaxed">
            Have questions, feedback, or just want to chat about your favorite
            games? We would love to hear from you. Reach out through any of our
            channels below.
          </p>
        </div>

        <div className="grid gap-6 sm:grid-cols-2 max-w-3xl mx-auto mt-12">
          <a
            href="mailto:gseyzeriat1@gmail.com"
            className="flex items-center gap-5 rounded-xl border p-6 hover:bg-muted/50 transition-colors group"
          >
            <div className="flex size-14 shrink-0 items-center justify-center rounded-full bg-primary/10 group-hover:bg-primary/20 transition-colors">
              <Mail className="size-7 text-primary" />
            </div>
            <div>
              <h3 className="font-semibold text-lg">Email Us</h3>
              <p className="text-muted-foreground">gseyzeriat1@gmail.com</p>
            </div>
          </a>

          <a
            href="https://github.com/gauthier-se/checkpoint"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-5 rounded-xl border p-6 hover:bg-muted/50 transition-colors group"
          >
            <div className="flex size-14 shrink-0 items-center justify-center rounded-full bg-primary/10 group-hover:bg-primary/20 transition-colors">
              <Github className="size-7 text-primary" />
            </div>
            <div>
              <h3 className="font-semibold text-lg">Open Source</h3>
              <p className="text-muted-foreground">View on GitHub</p>
            </div>
          </a>
        </div>
      </section>
    </div>
  )
}
