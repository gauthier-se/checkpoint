import { Link } from '@tanstack/react-router'
import { ArrowLeft, ArrowRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ButtonGroup } from '@/components/ui/button-group'
import { getPageNumbers } from '@/lib/pagination'

interface ComparePaginationProps {
  page: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

/**
 * Pagination for the profile comparison common-games list. Uses relative ("." )
 * navigation so it stays bound to the current compare route.
 */
export function ComparePagination({
  page,
  totalPages,
  hasNext,
  hasPrevious,
}: ComparePaginationProps) {
  if (totalPages <= 1) {
    return null
  }

  return (
    <div className="flex items-center justify-center gap-2 pt-6 pb-4">
      <Link to="." search={{ page: page - 1 }} disabled={!hasPrevious}>
        <Button variant="outline" size="sm" disabled={!hasPrevious}>
          <ArrowLeft className="size-4" />
          Previous
        </Button>
      </Link>
      <ButtonGroup>
        {getPageNumbers(page, totalPages).map((p, i) =>
          p === '...' ? (
            <Button key={`ellipsis-${i}`} variant="outline" size="sm" disabled>
              …
            </Button>
          ) : (
            <Link key={p} to="." search={{ page: p }}>
              <Button variant={p === page ? 'default' : 'outline'} size="sm">
                {p}
              </Button>
            </Link>
          ),
        )}
      </ButtonGroup>
      <Link to="." search={{ page: page + 1 }} disabled={!hasNext}>
        <Button variant="outline" size="sm" disabled={!hasNext}>
          Next
          <ArrowRight className="size-4" />
        </Button>
      </Link>
    </div>
  )
}
