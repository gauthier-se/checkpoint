import { Link } from '@tanstack/react-router'
import type { MemberCard as MemberCardType } from '@/types/member'
import { MemberCard } from '@/components/members/member-card'
import { SectionHeader } from '@/components/home/section-header'

export function SuggestedMembersSection({
  members,
  isLoading,
}: {
  members: Array<MemberCardType> | undefined
  isLoading: boolean
}) {
  if (isLoading || !members || members.length === 0) return null

  return (
    <section className="my-12">
      <SectionHeader
        title="People you might know"
        action={
          <Link
            to="/members/all"
            search={{ page: 1 }}
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            See all
          </Link>
        }
      />
      <div className="grid grid-cols-2 gap-4 py-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
        {members.map((member) => (
          <MemberCard key={member.id} member={member} />
        ))}
      </div>
    </section>
  )
}
