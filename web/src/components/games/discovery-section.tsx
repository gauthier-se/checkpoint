import type { ReactNode } from 'react'
import { Separator } from '@/components/ui/separator'

interface DiscoverySectionProps {
  title: string
  action?: ReactNode
  children: ReactNode
}

export function DiscoverySection({
  title,
  action,
  children,
}: DiscoverySectionProps) {
  return (
    <section className="my-8">
      <div className="flex items-center justify-between py-2">
        <h2 className="text-muted-foreground font-semibold">{title}</h2>
        {action}
      </div>
      <Separator />
      {children}
    </section>
  )
}
