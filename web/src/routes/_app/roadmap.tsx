import { createFileRoute } from '@tanstack/react-router'
import {
  Languages,
  MessageCircle,
  Smartphone,
  UserPlus,
  Users,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

import { seo } from '@/lib/seo'

interface RoadmapItem {
  label: string
  icon: LucideIcon
}

interface RoadmapPhase {
  stage: string
  description: string
  items: Array<RoadmapItem>
}

const PHASES: Array<RoadmapPhase> = [
  {
    stage: 'Short term',
    description: 'What we are focused on right now.',
    items: [
      { label: 'Translating the app into more languages', icon: Languages },
    ],
  },
  {
    stage: 'Medium term',
    description: 'Features planned once the foundations are in place.',
    items: [
      { label: 'Collaborative lists with friends', icon: Users },
      { label: 'Follow your favorite studios and developers', icon: UserPlus },
    ],
  },
  {
    stage: 'Long term',
    description: 'Our vision for the longer run.',
    items: [
      { label: 'Private messaging between users', icon: MessageCircle },
      { label: 'Native mobile app (iOS & Android)', icon: Smartphone },
    ],
  },
]

export const Route = createFileRoute('/_app/roadmap')({
  head: () => ({
    meta: seo({
      title: 'Roadmap — Checkpoint',
      description: 'Discover what we are building next for Checkpoint.',
    }),
  }),
  component: RoadmapPage,
})

export function RoadmapPage() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-16">
      <div className="max-w-3xl">
        <h1 className="mb-2 text-3xl font-bold tracking-tight">Roadmap</h1>
        <p className="text-muted-foreground mb-12">
          Here is what we have planned for Checkpoint.
        </p>

        <div className="relative">
          {/* Vertical timeline line */}
          <div className="bg-border absolute top-2 bottom-2 left-[7px] w-px" />

          <div className="space-y-10">
            {PHASES.map((phase) => (
              <div key={phase.stage} className="relative pl-8">
                {/* Timeline marker */}
                <span className="border-primary bg-background absolute top-1.5 left-0 size-3.5 rounded-full border-2" />

                <div className="mb-4">
                  <h2 className="text-xl font-semibold">{phase.stage}</h2>
                  <p className="text-muted-foreground text-sm">
                    {phase.description}
                  </p>
                </div>

                <ul className="space-y-3">
                  {phase.items.map((item) => {
                    const Icon = item.icon
                    return (
                      <li
                        key={item.label}
                        className="bg-card hover:border-primary/50 flex items-center gap-3 rounded-xl border p-4 transition-colors"
                      >
                        <span className="bg-primary/10 text-primary flex size-9 shrink-0 items-center justify-center rounded-full">
                          <Icon className="size-4" />
                        </span>
                        <span>{item.label}</span>
                      </li>
                    )
                  })}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
