import { Link, createFileRoute } from '@tanstack/react-router'
import { ArrowLeft } from 'lucide-react'
import { ListForm } from '@/components/lists/list-form'

import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/_protected/lists/new')({
  head: () => ({
    meta: seo({ title: 'New list — Checkpoint' }),
  }),
  component: RouteComponent,
})

function RouteComponent() {
  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <Link
        to="/lists"
        search={{ page: 1 }}
        className="mb-6 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        Back to lists
      </Link>

      <h1 className="text-3xl font-bold mb-8">Create a new list</h1>

      <ListForm mode="create" />
    </main>
  )
}
