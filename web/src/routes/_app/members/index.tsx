import { Link, createFileRoute, redirect } from '@tanstack/react-router'
import { useQuery, useSuspenseQuery } from '@tanstack/react-query'
import type { MemberCard as MemberCardType } from '@/types/member'
import { DiscoverySection } from '@/components/games/discovery-section'
import { MemberCard } from '@/components/members/member-card'
import {
  popularMembersQueryOptions,
  recentMembersQueryOptions,
  suggestedMembersQueryOptions,
  topReviewersMembersQueryOptions,
} from '@/queries/members'
import { useAuth } from '@/hooks/use-auth'

import { seo } from '@/lib/seo'
import { parseOptionalString } from '@/lib/search-params'

export const Route = createFileRoute('/_app/members/')({
  head: () => ({
    meta: seo({ title: 'Members — Checkpoint' }),
  }),
  component: RouteComponent,
  beforeLoad: ({ search }) => {
    // Preserve stale bookmarks: ?search/?page used to live on /members,
    // now they belong to /members/all.
    const raw = search as Record<string, unknown>
    const legacySearch = parseOptionalString(raw.search)
    const legacyPage = Number(raw.page)
    if (
      legacySearch !== undefined ||
      (Number.isFinite(legacyPage) && legacyPage > 0)
    ) {
      throw redirect({
        to: '/members/all',
        search: {
          page:
            Number.isFinite(legacyPage) && legacyPage > 0
              ? Math.floor(legacyPage)
              : 1,
          search: legacySearch,
        },
      })
    }
  },
  loader: async ({ context }) => {
    await Promise.all([
      context.queryClient.ensureQueryData(popularMembersQueryOptions(10)),
      context.queryClient.ensureQueryData(topReviewersMembersQueryOptions(10)),
      context.queryClient.ensureQueryData(recentMembersQueryOptions(10)),
    ])
  },
})

function MoreLink() {
  return (
    <Link
      to="/members/all"
      search={{ page: 1 }}
      className="text-sm text-muted-foreground hover:text-foreground"
    >
      More
    </Link>
  )
}

function MemberCardGrid({ members }: { members: Array<MemberCardType> }) {
  return (
    <div className="grid grid-cols-2 gap-4 py-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
      {members.map((member) => (
        <MemberCard key={member.id} member={member} />
      ))}
    </div>
  )
}

function RouteComponent() {
  const { user } = useAuth()

  const { data: popularMembers } = useSuspenseQuery(
    popularMembersQueryOptions(10),
  )
  const { data: topReviewers } = useSuspenseQuery(
    topReviewersMembersQueryOptions(10),
  )
  const { data: recentMembers } = useSuspenseQuery(
    recentMembersQueryOptions(10),
  )
  const { data: suggestedMembers } = useQuery({
    ...suggestedMembersQueryOptions(10),
    enabled: user !== null,
  })

  return (
    <div className="max-w-7xl mx-auto">
      <div className="mt-10 py-2">
        <h1 className="text-xl font-bold text-foreground">Members</h1>
      </div>

      <DiscoverySection title="Popular Members" action={<MoreLink />}>
        {popularMembers.length > 0 ? (
          <MemberCardGrid members={popularMembers} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No popular members yet.
          </p>
        )}
      </DiscoverySection>

      <DiscoverySection title="Top Reviewers" action={<MoreLink />}>
        {topReviewers.length > 0 ? (
          <MemberCardGrid members={topReviewers} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No top reviewers yet.
          </p>
        )}
      </DiscoverySection>

      {user && suggestedMembers && suggestedMembers.length > 0 && (
        <DiscoverySection title="Suggested Members" action={<MoreLink />}>
          <MemberCardGrid members={suggestedMembers} />
        </DiscoverySection>
      )}

      <DiscoverySection title="Recent Members" action={<MoreLink />}>
        {recentMembers.length > 0 ? (
          <MemberCardGrid members={recentMembers} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No recent members yet.
          </p>
        )}
      </DiscoverySection>
    </div>
  )
}
