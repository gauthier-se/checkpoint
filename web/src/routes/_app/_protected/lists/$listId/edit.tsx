import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { useEffect } from 'react'
import { listDetailQueryOptions } from '@/queries/lists'
import { ListForm } from '@/components/lists/list-form'

import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/_protected/lists/$listId/edit')({
  head: () => ({
    meta: seo({ title: 'Edit list — Checkpoint' }),
  }),
  component: RouteComponent,
  loader: async ({ params: { listId }, context }) => {
    await context.queryClient.ensureQueryData(listDetailQueryOptions(listId))
  },
})

function RouteComponent() {
  const { listId } = Route.useParams()
  const { data: list } = useSuspenseQuery(listDetailQueryOptions(listId))
  const navigate = useNavigate()

  useEffect(() => {
    if (!list.isOwner) {
      void navigate({ to: '/lists/$listId', params: { listId } })
    }
  }, [list.isOwner, listId, navigate])

  if (!list.isOwner) return null

  return (
    <main className="mx-auto max-w-3xl px-4 py-10">
      <Link
        to="/lists/$listId"
        params={{ listId }}
        className="mb-6 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        Back to list
      </Link>

      <h1 className="text-3xl font-bold mb-8">Edit list</h1>

      <ListForm mode="edit" initialData={list} />
    </main>
  )
}
