import { Link } from '@tanstack/react-router'
import { ArrowLeft, ArrowRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ButtonGroup } from '@/components/ui/button-group'
import { getPageNumbers } from '@/lib/pagination'

interface PopularWithFriendsPaginationProps {
  page: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export function PopularWithFriendsPagination({
  page,
  totalPages,
  hasNext,
  hasPrevious,
}: PopularWithFriendsPaginationProps) {
  return (
    <div className="flex items-center justify-between mt-6 mb-10">
      <Link
        to="/games/popular-with-friends"
        search={{ page: page - 1 }}
        disabled={!hasPrevious}
      >
        <Button variant="outline" disabled={!hasPrevious}>
          <ArrowLeft />
          Previous
        </Button>
      </Link>
      <ButtonGroup>
        {getPageNumbers(page, totalPages).map((p, i) =>
          p === '...' ? (
            <Button key={`ellipsis-${i}`} variant="outline" disabled>
              ...
            </Button>
          ) : (
            <Link key={p} to="/games/popular-with-friends" search={{ page: p }}>
              <Button variant={p === page ? 'default' : 'outline'}>{p}</Button>
            </Link>
          ),
        )}
      </ButtonGroup>
      <Link
        to="/games/popular-with-friends"
        search={{ page: page + 1 }}
        disabled={!hasNext}
      >
        <Button variant="outline" disabled={!hasNext}>
          Next
          <ArrowRight />
        </Button>
      </Link>
    </div>
  )
}
