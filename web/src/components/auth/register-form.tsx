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
import { Checkbox } from '@/components/ui/checkbox'
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldSeparator,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'
import { apiFetch } from '@/services/api'

const API_URL = import.meta.env.VITE_API_URL ?? ''

function startOAuthLogin(provider: 'google' | 'twitch') {
  // OAuth must go directly to the API origin (see login-form.tsx for context).
  window.location.href = `${API_URL}/api/oauth2/authorization/${provider}`
}

export function RegisterForm({
  className,
  ...props
}: React.ComponentProps<'div'>) {
  const navigate = useNavigate()

  const registerSchema = z
    .object({
      pseudo: z.string().min(1, 'Username is required'),
      email: z.email('Please enter a valid email address'),
      password: z
        .string()
        .min(8, 'Password must be at least 8 characters long'),
      confirmPassword: z.string().min(1, 'Password confirmation is required'),
      acceptTerms: z.boolean().refine((v) => v === true, {
        message: 'You must accept the Terms of Service and Privacy Policy',
      }),
    })
    .refine((data) => data.password === data.confirmPassword, {
      message: 'Passwords do not match',
      path: ['confirmPassword'],
    })

  const form = useForm({
    defaultValues: {
      pseudo: '',
      email: '',
      password: '',
      confirmPassword: '',
      acceptTerms: false,
    },
    validators: {
      onChange: registerSchema,
    },
    onSubmit: async ({ value }) => {
      const { acceptTerms: _acceptTerms, ...payload } = value
      const res = await apiFetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })

      if (!res.ok) {
        const data = await res.json().catch(() => null)
        toast.error(data?.message ?? 'Registration failed. Please try again.')
        return
      }

      await navigate({ to: '/login' })
    },
  })

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
                      <FieldLabel
                        htmlFor="acceptTerms"
                        className="text-sm font-normal leading-snug"
                      >
                        I agree to the{' '}
                        <Link to="/legal" className="underline">
                          Terms of Service
                        </Link>{' '}
                        and the{' '}
                        <Link to="/legal" hash="privacy" className="underline">
                          Privacy Policy
                        </Link>
                      </FieldLabel>
                    </div>
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
        </CardContent>
      </Card>
    </div>
  )
}
