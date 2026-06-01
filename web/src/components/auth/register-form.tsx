import { useForm } from '@tanstack/react-form'
import { Link, useNavigate } from '@tanstack/react-router'
import { toast } from 'sonner'
import { z } from 'zod'
import type { SteamSignupPrefill } from '@/types/auth'
import { API_PREFIX } from '@/services/api-config'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
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
import { apiFetch, isApiError } from '@/services/api'

const API_URL = import.meta.env.VITE_API_URL ?? ''

function startOAuthLogin(provider: 'google' | 'twitch') {
  // OAuth must go directly to the API origin (see login-form.tsx for context).
  window.location.href = `${API_URL}${API_PREFIX}/oauth2/authorization/${provider}`
}

function startSteamSignup() {
  // Steam OpenID is stateless; route through Nitro to keep cookies on the web origin.
  window.location.href = `${API_PREFIX}/auth/steam/openid/start?action=signup`
}

const standardSchema = z
  .object({
    pseudo: z.string().min(1, 'Username is required'),
    email: z.email('Please enter a valid email address'),
    password: z.string().min(8, 'Password must be at least 8 characters long'),
    confirmPassword: z.string().min(1, 'Password confirmation is required'),
    acceptTerms: z.boolean().refine((v) => v === true, {
      message: 'You must accept the Terms of Service and Privacy Policy',
    }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

const steamSchema = z
  .object({
    pseudo: z.string().min(1, 'Username is required'),
    email: z.email('Please enter a valid email address'),
    password: z.string().refine((v) => v === '' || v.length >= 8, {
      message: 'Password must be at least 8 characters long',
    }),
    confirmPassword: z.string(),
    acceptTerms: z.boolean().refine((v) => v === true, {
      message: 'You must accept the Terms of Service and Privacy Policy',
    }),
  })
  .refine(
    (data) => data.password === '' || data.password === data.confirmPassword,
    {
      message: 'Passwords do not match',
      path: ['confirmPassword'],
    },
  )

type RegisterFormProps = React.ComponentProps<'div'> & {
  steamToken?: string
  steamPrefill?: SteamSignupPrefill
  isSteamPrefillLoading?: boolean
}

export function RegisterForm({
  className,
  steamToken,
  steamPrefill,
  isSteamPrefillLoading,
  ...props
}: RegisterFormProps) {
  const navigate = useNavigate()
  const { invalidate } = useAuth()
  const isSteamMode = !!steamToken && !!steamPrefill

  return (
    <div className={cn('flex flex-col gap-6', className)} {...props}>
      <Card>
        <CardHeader className="text-center">
          <CardTitle className="text-xl">Create an account</CardTitle>
          <CardDescription>
            Enter your details below to get started
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isSteamPrefillLoading ? (
            <p className="text-sm text-muted-foreground">
              Loading your Steam profile…
            </p>
          ) : isSteamMode ? (
            <SteamRegisterForm
              prefill={steamPrefill}
              token={steamToken}
              onSuccess={async () => {
                await invalidate()
                await navigate({ to: '/' })
              }}
            />
          ) : (
            <StandardRegisterForm
              onSuccess={async () => {
                await navigate({ to: '/login' })
              }}
            />
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function OAuthButtons() {
  return (
    <Field>
      <div className="grid grid-cols-3 gap-2">
        <Button
          variant="outline"
          type="button"
          onClick={() => startOAuthLogin('google')}
          title="Continue with Google"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            className="size-5 shrink-0"
          >
            <path
              d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              fill="#4285F4"
            />
            <path
              d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              fill="#34A853"
            />
            <path
              d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"
              fill="#FBBC05"
            />
            <path
              d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
              fill="#EA4335"
            />
          </svg>
          Google
        </Button>
        <Button
          variant="outline"
          type="button"
          onClick={() => startOAuthLogin('twitch')}
          title="Continue with Twitch"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            className="size-5 shrink-0"
            fill="#9146FF"
          >
            <path d="M11.571 4.714h1.715v5.143H11.57zm4.715 0H18v5.143h-1.714zM6 0L1.714 4.286v15.428h5.143V24l4.286-4.286h3.428L22.286 12V0zm14.571 11.143l-3.428 3.428h-3.429l-3 3v-3H6.857V1.714h13.714Z" />
          </svg>
          Twitch
        </Button>
        <Button
          variant="outline"
          type="button"
          onClick={startSteamSignup}
          title="Continue with Steam"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            className="size-5 shrink-0"
            fill="#1b9ef3"
          >
            <path d="M11.979 0C5.678 0 .511 4.86.022 11.037l6.432 2.658c.545-.371 1.203-.59 1.912-.59.063 0 .125.004.188.006l2.861-4.142V8.91c0-2.495 2.028-4.524 4.524-4.524 2.494 0 4.524 2.031 4.524 4.527 0 2.498-2.03 4.527-4.524 4.527h-.105l-4.076 2.911c0 .052.004.105.004.159 0 1.875-1.515 3.396-3.39 3.396-1.635 0-3.016-1.173-3.331-2.727L.436 15.27C1.862 20.307 6.486 24 11.979 24c6.627 0 11.999-5.373 11.999-12S18.605 0 11.979 0zM7.54 18.21l-1.473-.61c.262.543.714.999 1.314 1.25 1.297.539 2.793-.076 3.332-1.375.263-.63.264-1.319.005-1.949s-.75-1.121-1.377-1.383c-.624-.26-1.29-.249-1.878-.03l1.523.63c.956.4 1.409 1.5 1.009 2.455-.397.957-1.497 1.41-2.454 1.012H7.54zm11.415-9.303c0-1.662-1.353-3.015-3.015-3.015-1.665 0-3.015 1.353-3.015 3.015 0 1.665 1.35 3.015 3.015 3.015 1.663 0 3.015-1.35 3.015-3.015zm-5.273-.005c0-1.252 1.013-2.266 2.265-2.266 1.249 0 2.266 1.014 2.266 2.266 0 1.251-1.017 2.265-2.266 2.265-1.253 0-2.265-1.014-2.265-2.265z" />
          </svg>
          Steam
        </Button>
      </div>
    </Field>
  )
}

function FieldErrors({
  errors,
}: {
  errors: Array<string | { message?: string }>
}) {
  if (errors.length === 0) return null
  return (
    <p className="text-sm text-destructive">
      {errors
        .map((e) => (typeof e === 'string' ? e : (e.message ?? '')))
        .join(', ')}
    </p>
  )
}

function StandardRegisterForm({
  onSuccess,
}: {
  onSuccess: () => Promise<void>
}) {
  const form = useForm({
    defaultValues: {
      pseudo: '',
      email: '',
      password: '',
      confirmPassword: '',
      acceptTerms: false,
    },
    validators: { onChange: standardSchema },
    onSubmit: async ({ value }) => {
      const { acceptTerms: _acceptTerms, ...payload } = value
      try {
        await apiFetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        })
      } catch (err) {
        toast.error(
          isApiError(err)
            ? err.message
            : 'Registration failed. Please try again.',
        )
        return
      }
      await onSuccess()
    },
  })

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        e.stopPropagation()
        form.handleSubmit()
      }}
    >
      <FieldGroup>
        <OAuthButtons />
        <FieldSeparator className="*:data-[slot=field-separator-content]:bg-card">
          Or continue with email
        </FieldSeparator>
        <form.Field
          name="pseudo"
          children={(field) => (
            <Field>
              <FieldLabel htmlFor="pseudo">Username</FieldLabel>
              <Input
                id="pseudo"
                name="pseudo"
                type="text"
                placeholder="Your username"
                autoComplete="username"
                required
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              <FieldErrors errors={field.state.meta.errors} />
            </Field>
          )}
        />
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
              <FieldErrors errors={field.state.meta.errors} />
            </Field>
          )}
        />
        <form.Field
          name="password"
          children={(field) => (
            <Field>
              <FieldLabel htmlFor="password">Password</FieldLabel>
              <Input
                id="password"
                name="password"
                type="password"
                placeholder="At least 8 characters"
                autoComplete="new-password"
                minLength={8}
                required
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              <FieldErrors errors={field.state.meta.errors} />
            </Field>
          )}
        />
        <form.Field
          name="confirmPassword"
          children={(field) => (
            <Field>
              <FieldLabel htmlFor="confirmPassword">
                Confirm password
              </FieldLabel>
              <Input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                placeholder="Repeat your password"
                autoComplete="new-password"
                minLength={8}
                required
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              <FieldErrors errors={field.state.meta.errors} />
            </Field>
          )}
        />
        <form.Field
          name="acceptTerms"
          children={(field) => (
            <Field>
              <div className="flex items-start gap-2">
                <Checkbox
                  id="acceptTerms"
                  checked={field.state.value}
                  onCheckedChange={(checked) =>
                    field.handleChange(checked === true)
                  }
                />
                <label
                  htmlFor="acceptTerms"
                  className="text-sm font-normal leading-snug cursor-pointer"
                >
                  I agree to the{' '}
                  <Link to="/legal" className="underline underline-offset-4">
                    Terms of Service
                  </Link>{' '}
                  and the{' '}
                  <Link
                    to="/legal"
                    hash="privacy"
                    className="underline underline-offset-4"
                  >
                    Privacy Policy
                  </Link>
                </label>
              </div>
              <FieldErrors errors={field.state.meta.errors} />
            </Field>
          )}
        />
        <Field>
          <form.Subscribe
            selector={(state) => [state.canSubmit, state.isSubmitting]}
            children={([canSubmit, isSubmitting]) => (
              <Button type="submit" disabled={!canSubmit || isSubmitting}>
                {isSubmitting ? 'Creating account...' : 'Create account'}
              </Button>
            )}
          />
          <FieldDescription className="text-center">
            Already have an account? <Link to="/login">Sign in</Link>
          </FieldDescription>
        </Field>
      </FieldGroup>
    </form>
  )
}

function SteamRegisterForm({
  prefill,
  token,
  onSuccess,
}: {
  prefill: SteamSignupPrefill
  token: string
  onSuccess: () => Promise<void>
}) {
  const initialPseudo = (prefill.steamDisplayName ?? '').trim()
  const displayName = prefill.steamDisplayName ?? 'your Steam account'
  const initials = initialPseudo.slice(0, 2).toUpperCase() || 'ST'

  const form = useForm({
    defaultValues: {
      pseudo: initialPseudo,
      email: '',
      password: '',
      confirmPassword: '',
      acceptTerms: false,
    },
    validators: { onChange: steamSchema },
    onSubmit: async ({ value }) => {
      const payload = {
        token,
        email: value.email,
        pseudo: value.pseudo,
        acceptTerms: value.acceptTerms,
        password: value.password || undefined,
      }
      try {
        await apiFetch('/api/auth/register/steam', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        })
      } catch (err) {
        toast.error(
          isApiError(err)
            ? err.message
            : 'Registration failed. Please try again.',
        )
        return
      }
      await onSuccess()
    },
  })

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        e.stopPropagation()
        form.handleSubmit()
      }}
    >
      <FieldGroup>
        <div className="flex items-center gap-3 rounded-md border border-border bg-muted/50 p-3">
          <Avatar size="lg">
            {prefill.steamAvatarUrl ? (
              <AvatarImage src={prefill.steamAvatarUrl} alt={displayName} />
            ) : null}
            <AvatarFallback>{initials}</AvatarFallback>
          </Avatar>
          <div className="text-sm">
            <p className="font-medium">Creating your account from Steam</p>
            <p className="text-muted-foreground">{displayName}</p>
          </div>
        </div>
        <form.Field
          name="pseudo"
          children={(field) => (
            <Field>
              <FieldLabel htmlFor="pseudo">Username</FieldLabel>
              <Input
                id="pseudo"
                name="pseudo"
                type="text"
                placeholder="Your username"
                autoComplete="username"
                required
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              <FieldErrors errors={field.state.meta.errors} />
            </Field>
          )}
        />
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
              <FieldErrors errors={field.state.meta.errors} />
            </Field>
          )}
        />
        <form.Field
          name="password"
          children={(field) => (
            <Field>
              <FieldLabel htmlFor="password">Password (optional)</FieldLabel>
              <Input
                id="password"
                name="password"
                type="password"
                placeholder="At least 8 characters"
                autoComplete="new-password"
                minLength={8}
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              <FieldDescription>
                Leave blank to sign in with Steam only.
              </FieldDescription>
              <FieldErrors errors={field.state.meta.errors} />
            </Field>
          )}
        />
        <form.Subscribe
          selector={(state) => state.values.password}
          children={(password) =>
            password.length > 0 ? (
              <form.Field
                name="confirmPassword"
                children={(field) => (
                  <Field>
                    <FieldLabel htmlFor="confirmPassword">
                      Confirm password
                    </FieldLabel>
                    <Input
                      id="confirmPassword"
                      name="confirmPassword"
                      type="password"
                      placeholder="Repeat your password"
                      autoComplete="new-password"
                      minLength={8}
                      required
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(e) => field.handleChange(e.target.value)}
                    />
                    <FieldErrors errors={field.state.meta.errors} />
                  </Field>
                )}
              />
            ) : null
          }
        />
        <form.Field
          name="acceptTerms"
          children={(field) => (
            <Field>
              <div className="flex items-start gap-2">
                <Checkbox
                  id="acceptTerms"
                  checked={field.state.value}
                  onCheckedChange={(checked) =>
                    field.handleChange(checked === true)
                  }
                />
                <label
                  htmlFor="acceptTerms"
                  className="text-sm font-normal leading-snug cursor-pointer"
                >
                  I agree to the{' '}
                  <Link to="/legal" className="underline underline-offset-4">
                    Terms of Service
                  </Link>{' '}
                  and the{' '}
                  <Link
                    to="/legal"
                    hash="privacy"
                    className="underline underline-offset-4"
                  >
                    Privacy Policy
                  </Link>
                </label>
              </div>
              <FieldErrors errors={field.state.meta.errors} />
            </Field>
          )}
        />
        <Field>
          <form.Subscribe
            selector={(state) => [state.canSubmit, state.isSubmitting]}
            children={([canSubmit, isSubmitting]) => (
              <Button type="submit" disabled={!canSubmit || isSubmitting}>
                {isSubmitting ? 'Creating account...' : 'Create account'}
              </Button>
            )}
          />
          <FieldDescription className="text-center">
            Already have an account? <Link to="/login">Sign in</Link>
          </FieldDescription>
        </Field>
      </FieldGroup>
    </form>
  )
}
