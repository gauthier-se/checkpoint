import { useEffect } from 'react'
import { Link } from '@tanstack/react-router'
import {
  AlertTriangle,
  Lock,
  SearchX,
  ServerCrash,
  ShieldX,
  WifiOff,
  Wrench,
} from 'lucide-react'
import type { ComponentType } from 'react'
import type { LucideProps } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface ErrorPageProps {
  status?: number
  title?: string
  message?: string
  onRetry?: () => void
}

interface StatusContent {
  icon: ComponentType<LucideProps>
  title: string
  message: string
  showRetry: boolean
  showSignIn?: boolean
}

function getStatusContent(status: number | undefined): StatusContent {
  switch (status) {
    case 404:
      return {
        icon: SearchX,
        title: 'Page not found',
        message: "We can't find what you were looking for.",
        showRetry: false,
      }
    case 403:
      return {
        icon: ShieldX,
        title: 'Access denied',
        message: "You don't have permission to access this page.",
        showRetry: false,
      }
    case 401:
      return {
        icon: Lock,
        title: 'Sign in required',
        message: 'You need to sign in to continue.',
        showRetry: false,
        showSignIn: true,
      }
    case 503:
      return {
        icon: Wrench,
        title: 'Service unavailable',
        message:
          'Checkpoint is temporarily down for maintenance. Please check back soon.',
        showRetry: true,
      }
    case 0:
      return {
        icon: WifiOff,
        title: "Can't reach the server",
        message: 'Check your connection and try again.',
        showRetry: true,
      }
    default:
      if (status !== undefined && status >= 500 && status < 600) {
        return {
          icon: ServerCrash,
          title: 'Server error',
          message: 'Something went wrong on our end. Please try again later.',
          showRetry: true,
        }
      }
      return {
        icon: AlertTriangle,
        title: 'Something went wrong',
        message: 'An unexpected error occurred. Please try again.',
        showRetry: true,
      }
  }
}

export function ErrorPage({ status, title, message, onRetry }: ErrorPageProps) {
  const content = getStatusContent(status)
  const resolvedTitle = title ?? content.title
  const resolvedMessage = message ?? content.message
  const Icon = content.icon

  useEffect(() => {
    if (typeof document === 'undefined') return
    const previous = document.title
    document.title = `${resolvedTitle} - Checkpoint`
    return () => {
      document.title = previous
    }
  }, [resolvedTitle])

  return (
    <div className="min-h-[60vh] flex items-center justify-center px-4">
      <div className="max-w-md w-full text-center flex flex-col items-center gap-4">
        <div className="rounded-full bg-destructive/10 p-3 text-destructive">
          <Icon className="h-8 w-8" />
        </div>
        <h1 className="text-2xl font-bold">{resolvedTitle}</h1>
        <p className="text-muted-foreground">{resolvedMessage}</p>
        <div className="flex gap-2 mt-2">
          {content.showSignIn && (
            <Button asChild variant="default">
              <Link to="/login">Sign in</Link>
            </Button>
          )}
          {content.showRetry && onRetry && (
            <Button onClick={onRetry} variant="default">
              Try again
            </Button>
          )}
          <Button asChild variant="outline">
            <Link to="/">Go home</Link>
          </Button>
        </div>
      </div>
    </div>
  )
}
