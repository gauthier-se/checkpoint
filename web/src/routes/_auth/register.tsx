import { Link, createFileRoute } from '@tanstack/react-router'
import { queryOptions, useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { toast } from 'sonner'
import type { SteamSignupPrefill } from '@/types/auth'
import { RegisterForm } from '@/components/auth/register-form'
import { apiFetch, isApiError } from '@/services/api'

import { seo } from '@/lib/seo'

type RegisterSearchParams = {
  steam_token?: string
}

function steamSignupPrefillQuery(token: string) {
  return queryOptions({
    queryKey: ['auth', 'steam-signup-prefill', token],
    queryFn: async (): Promise<SteamSignupPrefill> => {
      const res = await apiFetch(
        `/api/auth/steam/signup-prefill?token=${encodeURIComponent(token)}`,
      )
      return res.json()
    },
    retry: false,
    staleTime: Infinity,
  })
}

export const Route = createFileRoute('/_auth/register')({
  head: () => ({
    meta: seo({ title: 'Create account — Checkpoint' }),
  }),
  validateSearch: (search: Record<string, unknown>): RegisterSearchParams => ({
    steam_token:
      typeof search.steam_token === 'string' ? search.steam_token : undefined,
  }),
  component: RegisterPage,
})

function RegisterPage() {
  const { steam_token } = Route.useSearch()

  const prefill = useQuery({
    ...steamSignupPrefillQuery(steam_token ?? ''),
    enabled: !!steam_token,
  })

  useEffect(() => {
    if (prefill.isError) {
      toast.error(
        isApiError(prefill.error)
          ? 'Your Steam signup link is invalid or has expired. Please try again.'
          : 'Could not load Steam profile. Please try again.',
      )
    }
  }, [prefill.isError, prefill.error])

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
        <RegisterForm
          steamToken={steam_token}
          steamPrefill={prefill.data}
          isSteamPrefillLoading={!!steam_token && prefill.isLoading}
        />
      </div>
    </div>
  )
}
