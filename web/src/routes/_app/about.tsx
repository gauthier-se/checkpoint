import { createFileRoute } from '@tanstack/react-router'
import {
  Gamepad2,
  Instagram,
  ListChecks,
  Mail,
  MapPin,
  Star,
  Users,
} from 'lucide-react'

import { seo } from '@/lib/seo'

const XIcon = ({ className }: { className?: string }) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="currentColor"
    className={className}
  >
    <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
  </svg>
)

const DiscordIcon = ({ className }: { className?: string }) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="currentColor"
    className={className}
  >
    <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.095 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.095 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z" />
  </svg>
)

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
            href="mailto:hello@checkpoint.app"
            className="flex items-center gap-5 rounded-xl border p-6 hover:bg-muted/50 transition-colors group"
          >
            <div className="flex size-14 shrink-0 items-center justify-center rounded-full bg-primary/10 group-hover:bg-primary/20 transition-colors">
              <Mail className="size-7 text-primary" />
            </div>
            <div>
              <h3 className="font-semibold text-lg">Email Us</h3>
              <p className="text-muted-foreground">hello@checkpoint.app</p>
            </div>
          </a>

          <a
            href="https://discord.gg"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-5 rounded-xl border p-6 hover:bg-muted/50 transition-colors group"
          >
            <div className="flex size-14 shrink-0 items-center justify-center rounded-full bg-primary/10 group-hover:bg-primary/20 transition-colors">
              <DiscordIcon className="size-7 text-primary" />
            </div>
            <div>
              <h3 className="font-semibold text-lg">Join the Discord</h3>
              <p className="text-muted-foreground">Chat with the community</p>
            </div>
          </a>

          <a
            href="https://x.com"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-5 rounded-xl border p-6 hover:bg-muted/50 transition-colors group"
          >
            <div className="flex size-14 shrink-0 items-center justify-center rounded-full bg-primary/10 group-hover:bg-primary/20 transition-colors">
              <XIcon className="size-7 text-primary" />
            </div>
            <div>
              <h3 className="font-semibold text-lg">Follow on X</h3>
              <p className="text-muted-foreground">@checkpoint</p>
            </div>
          </a>

          <a
            href="https://instagram.com"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-5 rounded-xl border p-6 hover:bg-muted/50 transition-colors group"
          >
            <div className="flex size-14 shrink-0 items-center justify-center rounded-full bg-primary/10 group-hover:bg-primary/20 transition-colors">
              <Instagram className="size-7 text-primary" />
            </div>
            <div>
              <h3 className="font-semibold text-lg">Instagram</h3>
              <p className="text-muted-foreground">Behind the scenes</p>
            </div>
          </a>
        </div>
      </section>
    </div>
  )
}
