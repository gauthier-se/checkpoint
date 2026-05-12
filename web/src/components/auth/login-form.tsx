import { useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { Link, useNavigate } from '@tanstack/react-router'
import { toast } from 'sonner'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldSeparator,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/hooks/use-auth'
import { cn } from '@/lib/utils'
import { apiFetch } from '@/services/api'

interface LoginFormProps extends React.ComponentProps<'div'> {
  redirectTo?: string
}

const API_URL = import.meta.env.VITE_API_URL ?? ''

function startOAuthLogin(provider: 'google' | 'twitch') {
  // OAuth must go directly to the API origin: the OAuth state cookie set by
  // Spring Security on the authorization request must be readable on the
  // callback (registered with the provider as ${API_URL}/login/oauth2/code/*).
  // Trade-off: OAuth-issued auth cookies end up on the API origin, so the
  // SSR auth prefetch can't see them in cross-origin dev (flicker remains
  // for OAuth users only).
  window.location.href = `${API_URL}/api/oauth2/authorization/${provider}`
}

const loginSchema = z.object({
  email: z.email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

const totpSchema = z.object({
  code: z.string().length(6, 'Code must be exactly 6 digits'),
})

export function LoginForm({ className, redirectTo, ...props }: LoginFormProps) {
  const navigate = useNavigate()
  const { invalidate } = useAuth()
  const [twoFactorRequired, setTwoFactorRequired] = useState(false)

  const form = useForm({
    defaultValues: {
      email: '',
      password: '',
    },
    validators: {
      onChange: loginSchema,
    },
    onSubmit: async ({ value }) => {
      const res = await apiFetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(value),
      })

      if (!res.ok) {
        const data = await res.json().catch(() => null)
        toast.error(data?.message ?? 'Invalid email or password')
        return
      }

      const data = await res.json().catch(() => null)
      if (data?.twoFactorRequired) {
        setTwoFactorRequired(true)
        return
      }

      await invalidate()
      await navigate({ to: redirectTo ?? '/' })
    },
  })

  const totpForm = useForm({
    defaultValues: { code: '' },
    validators: { onSubmit: totpSchema },
    onSubmit: async ({ value }) => {
      const res = await apiFetch('/api/auth/2fa/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(value),
      })

      if (!res.ok) {
        const data = await res.json().catch(() => null)
        toast.error(data?.message ?? 'Invalid code. Please try again.')
        return
      }

      await invalidate()
      await navigate({ to: redirectTo ?? '/' })
    },
  })

  if (twoFactorRequired) {
    return (
      <div className={cn('flex flex-col gap-6', className)} {...props}>
        <Card>
          <CardHeader className="text-center">
            <CardTitle className="text-xl">Two-Factor Authentication</CardTitle>
            <CardDescription>
              Enter the 6-digit code from your authenticator app.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form
              onSubmit={(e) => {
                e.preventDefault()
                e.stopPropagation()
                totpForm.handleSubmit()
              }}
            >
              <FieldGroup>
                <totpForm.Field
                  name="code"
                  children={(field) => (
                    <Field>
                      <FieldLabel htmlFor="totp-code">
                        Authenticator code
                      </FieldLabel>
                      <Input
                        id="totp-code"
                        type="text"
                        inputMode="numeric"
                        maxLength={6}
                        placeholder="000000"
                        autoComplete="one-time-code"
                        autoFocus
                        value={field.state.value}
                        onBlur={field.handleBlur}
                        onChange={(e) => field.handleChange(e.target.value)}
                      />
                      {field.state.meta.errors.length > 0 && (
                        <p className="text-sm text-destructive">
                          {field.state.meta.errors
                            .map((e) =>
                              typeof e === 'string' ? e : (e as any).message,
                            )
                            .join(', ')}
                        </p>
                      )}
                    </Field>
                  )}
                />
                <Field>
                  <totpForm.Subscribe
                    selector={(state) => [state.canSubmit, state.isSubmitting]}
                    children={([canSubmit, isSubmitting]) => (
                      <Button
                        type="submit"
                        disabled={!canSubmit || isSubmitting}
                      >
                        {isSubmitting ? 'Verifying...' : 'Verify'}
                      </Button>
                    )}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setTwoFactorRequired(false)}
                  >
                    Back
                  </Button>
                </Field>
              </FieldGroup>
            </form>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className={cn('flex flex-col gap-6', className)} {...props}>
      <Card>
        <CardHeader className="text-center">
          <CardTitle className="text-xl">Welcome back</CardTitle>
          <CardDescription>
            Login with your Google or Twitch account
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={(e) => {
              e.preventDefault()
              e.stopPropagation()
              form.handleSubmit()
            }}
          >
            <FieldGroup>
              <Field>
                <Button
                  variant="outline"
                  type="button"
                  onClick={() => startOAuthLogin('google')}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                    <path
                      d="M12.48 10.92v3.28h7.84c-.24 1.84-.853 3.187-1.787 4.133-1.147 1.147-2.933 2.4-6.053 2.4-4.827 0-8.6-3.893-8.6-8.72s3.773-8.72 8.6-8.72c2.6 0 4.507 1.027 5.907 2.347l2.307-2.307C18.747 1.44 16.133 0 12.48 0 5.867 0 .307 5.387.307 12s5.56 12 12.173 12c3.573 0 6.267-1.173 8.373-3.36 2.16-2.16 2.84-5.213 2.84-7.667 0-.76-.053-1.467-.173-2.053H12.48z"
                      fill="currentColor"
                    />
                  </svg>
                  Continue with Google
                </Button>
                <Button
                  variant="outline"
                  type="button"
                  onClick={() => startOAuthLogin('twitch')}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                    <path
                      d="M11.571 4.714h1.715v5.143H11.57zm4.715 0H18v5.143h-1.714zM6 0L1.714 4.286v15.428h5.143V24l4.286-4.286h3.428L22.286 12V0zm14.571 11.143l-3.428 3.428h-3.429l-3 3v-3H6.857V1.714h13.714Z"
                      fill="currentColor"
                    />
                  </svg>
                  Continue with Twitch
                </Button>
              </Field>
              <FieldSeparator className="*:data-[slot=field-separator-content]:bg-card">
                Or continue with
              </FieldSeparator>
              <form.Field
                name="email"
                children={(field) => (
                  <Field>
                    <FieldLabel htmlFor="email">Email</FieldLabel>
                    <Input
                      id="email"
                      name="email"
                      type="email"
                      placeholder="you@example.com"
                      autoComplete="email"
                      required
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(e) => field.handleChange(e.target.value)}
                    />
                    {field.state.meta.errors.length > 0 && (
                      <p className="text-sm text-destructive">
                        {field.state.meta.errors
                          .map((e) =>
                            typeof e === 'string' ? e : (e as any).message,
                          )
                          .join(', ')}
                      </p>
                    )}
                  </Field>
                )}
              />
              <form.Field
                name="password"
                children={(field) => (
                  <Field>
                    <div className="flex items-center">
                      <FieldLabel htmlFor="password">Password</FieldLabel>
                      <Link
                        to="/forgot-password"
                        className="ml-auto text-sm underline-offset-4 hover:underline"
                      >
                        Forgot your password?
                      </Link>
                    </div>
                    <Input
                      id="password"
                      name="password"
                      type="password"
                      autoComplete="current-password"
                      required
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(e) => field.handleChange(e.target.value)}
                    />
                    {field.state.meta.errors.length > 0 && (
                      <p className="text-sm text-destructive">
                        {field.state.meta.errors
                          .map((e) =>
                            typeof e === 'string' ? e : (e as any).message,
                          )
                          .join(', ')}
                      </p>
                    )}
                  </Field>
                )}
              />

              <Field>
                <form.Subscribe
                  selector={(state) => [state.canSubmit, state.isSubmitting]}
                  children={([canSubmit, isSubmitting]) => (
                    <Button type="submit" disabled={!canSubmit || isSubmitting}>
                      {isSubmitting ? 'Signing in...' : 'Login'}
                    </Button>
                  )}
                />
                <FieldDescription className="text-center">
                  Don&apos;t have an account?{' '}
                  <Link to="/register">Sign up</Link>
                </FieldDescription>
              </Field>
            </FieldGroup>
          </form>
        </CardContent>
      </Card>
      <FieldDescription className="px-6 text-center">
        By clicking continue, you agree to our <a href="#">Terms of Service</a>{' '}
        and <a href="#">Privacy Policy</a>.
      </FieldDescription>
    </div>
  )
}
