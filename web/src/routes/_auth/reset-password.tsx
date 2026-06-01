import { Link, createFileRoute } from '@tanstack/react-router'
import { ResetPasswordForm } from '@/components/auth/reset-password-form'

import { seo } from '@/lib/seo'

type ResetPasswordSearchParams = {
  token?: string
}

export const Route = createFileRoute('/_auth/reset-password')({
  head: () => ({
    meta: seo({ title: 'Reset password — Checkpoint' }),
  }),
  validateSearch: (
    search: Record<string, unknown>,
  ): ResetPasswordSearchParams => ({
    token: typeof search.token === 'string' ? search.token : undefined,
  }),
  component: ResetPasswordPage,
})

function ResetPasswordPage() {
  const { token } = Route.useSearch()

  if (!token) {
    return (
      <div className="bg-background flex min-h-svh flex-col items-center justify-center gap-6 p-6 md:p-10">
        <div className="flex w-full max-w-sm flex-col gap-6 text-center">
          <Link
            to="/"
            className="flex items-center gap-2 self-center font-medium"
          >
            <img className="w-6" src="/images/logo.png" alt="Checkpoint" />
            Checkpoint
          </Link>
          <p className="text-sm text-muted-foreground">
            Invalid or missing reset token.{' '}
            <Link
              to="/forgot-password"
              className="underline underline-offset-4"
            >
              Request a new one
            </Link>
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-background flex min-h-svh flex-col items-center justify-center gap-6 p-6 md:p-10">
      <div className="flex w-full max-w-sm flex-col gap-6">
        <Link
          to="/"
          className="flex items-center gap-2 self-center font-medium"
        >
          <img className="w-6" src="/images/logo.png" alt="Checkpoint" />
          Checkpoint
        </Link>
        <ResetPasswordForm token={token} />
      </div>
    </div>
  )
}
