import { createFileRoute } from '@tanstack/react-router'
import { CheckCircle2, Clock, ListTodo } from 'lucide-react'

import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/roadmap')({
  head: () => ({
    meta: seo({ title: 'Roadmap — Checkpoint' }),
  }),
  component: RoadmapPage,
})

const roadmapData = {
  shipped: [
    {
      title: 'Roadmap Page',
      description:
        'A dedicated page outlining the past, present, and future of the platform.',
      date: 'April 2026',
    },
    {
      title: 'About & Contact Page',
      description:
        'A static page detailing the project mission and contact info.',
      date: 'April 2026',
    },
    {
      title: 'User Authentication',
      description:
        'Secure login and registration with email and social providers.',
      date: 'March 2026',
    },
  ],
  inProgress: [
    {
      title: 'Detailed Reviews',
      description:
        'Allow users to write long-form reviews with rich text formatting.',
    },
    {
      title: 'User Profiles',
      description:
        'Public and private user profiles showcasing collections and stats.',
    },
  ],
  planned: [
    {
      title: 'Social Features',
      description:
        'Follow other users, see their recent activity, and comment on reviews.',
    },
    {
      title: 'Custom Lists',
      description:
        'Create themed lists of games (e.g., "Best RPGs of 2025") and share them.',
    },
    {
      title: 'API Integration',
      description:
        'Automated data syncing for new game releases and platform updates.',
    },
  ],
}

function RoadmapPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-16 sm:py-24 space-y-20">
      {/* Header */}
      <div className="text-center space-y-6">
        <h1 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
          Development <span className="text-primary">Roadmap</span>
        </h1>
        <p className="mx-auto max-w-2xl text-lg text-muted-foreground sm:text-xl leading-relaxed">
          See what we've recently shipped, what we're currently building, and
          what's planned for the future of Checkpoint.
        </p>
      </div>

      <div className="space-y-16">
        {/* In Progress */}
        <section className="space-y-8">
          <div className="flex items-center gap-4">
            <div className="flex size-12 items-center justify-center rounded-full bg-blue-500/10 text-blue-500">
              <Clock className="size-6" />
            </div>
            <h2 className="text-3xl font-bold">In Progress</h2>
          </div>
          <div className="grid gap-6 sm:grid-cols-2">
            {roadmapData.inProgress.map((item) => (
              <div
                key={item.title}
                className="flex flex-col gap-3 rounded-xl border border-blue-500/20 p-6 bg-card text-card-foreground shadow-sm relative overflow-hidden transition-all hover:shadow-md"
              >
                <div className="absolute top-0 left-0 w-1 h-full bg-blue-500" />
                <div className="flex justify-between items-start gap-4">
                  <h3 className="font-semibold text-xl">{item.title}</h3>
                  <span className="inline-flex items-center rounded-full bg-blue-500/10 px-2.5 py-0.5 text-xs font-semibold text-blue-500 shrink-0">
                    Working on it
                  </span>
                </div>
                <p className="text-muted-foreground leading-relaxed">
                  {item.description}
                </p>
              </div>
            ))}
          </div>
        </section>

        {/* Planned */}
        <section className="space-y-8">
          <div className="flex items-center gap-4">
            <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
              <ListTodo className="size-6" />
            </div>
            <h2 className="text-3xl font-bold">Planned</h2>
          </div>
          <div className="grid gap-6 sm:grid-cols-2">
            {roadmapData.planned.map((item) => (
              <div
                key={item.title}
                className="flex flex-col gap-3 rounded-xl border p-6 bg-muted/30 text-card-foreground shadow-sm transition-all hover:shadow-md"
              >
                <div className="flex justify-between items-start gap-4">
                  <h3 className="font-semibold text-xl">{item.title}</h3>
                  <span className="inline-flex items-center rounded-full bg-muted px-2.5 py-0.5 text-xs font-semibold text-muted-foreground shrink-0">
                    Backlog
                  </span>
                </div>
                <p className="text-muted-foreground leading-relaxed">
                  {item.description}
                </p>
              </div>
            ))}
          </div>
        </section>

        {/* Shipped */}
        <section className="space-y-8">
          <div className="flex items-center gap-4">
            <div className="flex size-12 items-center justify-center rounded-full bg-green-500/10 text-green-500">
              <CheckCircle2 className="size-6" />
            </div>
            <h2 className="text-3xl font-bold">Recently Shipped</h2>
          </div>
          <div className="grid gap-6 sm:grid-cols-2">
            {roadmapData.shipped.map((item) => (
              <div
                key={item.title}
                className="flex flex-col gap-3 rounded-xl border border-green-500/20 p-6 bg-card text-card-foreground shadow-sm relative overflow-hidden transition-all hover:shadow-md"
              >
                <div className="absolute top-0 left-0 w-1 h-full bg-green-500" />
                <div className="flex justify-between items-start gap-4">
                  <h3 className="font-semibold text-xl">{item.title}</h3>
                  <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold text-muted-foreground shrink-0">
                    {item.date}
                  </span>
                </div>
                <p className="text-muted-foreground leading-relaxed">
                  {item.description}
                </p>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}
