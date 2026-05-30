import { Link, createFileRoute } from '@tanstack/react-router'
import { useEffect } from 'react'
import { toast } from 'sonner'
import { LoginForm } from '@/components/auth/login-form'

import { seo } from '@/lib/seo'

type LoginSearchParams = {
  redirect?: string
  error?: string
  twoFactorRequired?: boolean
}

const OAUTH_ERROR_MESSAGES: Record<string, string> = {
  oauth_failed: 'OAuth login failed. Please try again.',
  oauth_banned: 'This account has been banned.',
  steam_not_linked: 'Steam sign-in failed. Please try again.',
  steam_openid_failed: 'Steam sign-in failed. Please try again.',
  not_authenticated: 'You must be signed in to link a Steam account.',
}

export const Route = createFileRoute('/_auth/login')({
  head: () => ({
    meta: seo({ title: 'Sign in — Checkpoint' }),
  }),
  validateSearch: (search: Record<string, unknown>): LoginSearchParams => ({
    redirect: typeof search.redirect === 'string' ? search.redirect : undefined,
    error: typeof search.error === 'string' ? search.error : undefined,
    // Social logins (Google/Twitch/Steam) redirect here with `?2fa=required`
    // when the matched account has 2FA enabled, to prompt for the TOTP code.
    twoFactorRequired: search['2fa'] === 'required',
  }),
  component: LoginPage,
})

function LoginPage() {
  const { redirect, error, twoFactorRequired } = Route.useSearch()

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
        <LoginForm
          redirectTo={redirect}
          twoFactorRequired={twoFactorRequired}
        />
      </div>
    </div>
  )
}
