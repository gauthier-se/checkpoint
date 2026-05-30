import { Link, createFileRoute } from '@tanstack/react-router'
import { ForgotPasswordForm } from '@/components/auth/forgot-password-form'

import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_auth/forgot-password')({
  head: () => ({
    meta: seo({ title: 'Forgot password — Checkpoint' }),
  }),
  component: ForgotPasswordPage,
})

function ForgotPasswordPage() {
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
        <ForgotPasswordForm />
      </div>
    </div>
  )
}
