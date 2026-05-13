import { Link, createFileRoute } from '@tanstack/react-router'
import { useEffect } from 'react'
import { toast } from 'sonner'
import { LoginForm } from '@/components/auth/login-form'

type LoginSearchParams = {
  redirect?: string
  error?: string
}

const OAUTH_ERROR_MESSAGES: Record<string, string> = {
  oauth_failed: 'OAuth login failed. Please try again.',
  oauth_banned: 'This account has been banned.',
  steam_not_linked:
    'No CheckPoint account is linked to this Steam account. Create an account first, then link Steam in Settings.',
  steam_openid_failed: 'Steam sign-in failed. Please try again.',
  not_authenticated: 'You must be signed in to link a Steam account.',
}

export const Route = createFileRoute('/_auth/login')({
  validateSearch: (search: Record<string, unknown>): LoginSearchParams => ({
    redirect: typeof search.redirect === 'string' ? search.redirect : undefined,
    error: typeof search.error === 'string' ? search.error : undefined,
  }),
  component: LoginPage,
})

function LoginPage() {
  const { redirect, error } = Route.useSearch()

  useEffect(() => {
    if (error) {
      toast.error(
        OAUTH_ERROR_MESSAGES[error] ?? OAUTH_ERROR_MESSAGES.oauth_failed,
      )
    }
  }, [error])

  return (
    <div className="bg-muted flex min-h-svh flex-col items-center justify-center gap-6 p-6 md:p-10">
      <div className="flex w-full max-w-sm flex-col gap-6">
        <Link
          to="/"
          className="flex items-center gap-2 self-center font-medium"
        >
          <img className="w-6" src="/images/logo.png" alt="Checkpoint" />
          Checkpoint
        </Link>
        <LoginForm redirectTo={redirect} />
      </div>
    </div>
  )
}
