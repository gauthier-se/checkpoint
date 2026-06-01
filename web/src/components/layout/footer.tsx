import { Link } from '@tanstack/react-router'
import { Github, Keyboard } from 'lucide-react'

interface FooterProps {
  onOpenKeymaps?: () => void
}

export const Footer = ({ onOpenKeymaps }: FooterProps) => {
  return (
    <footer className="w-full bg-muted">
      <div className="mx-auto max-w-7xl px-4 py-10">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2 font-semibold">
            <Link to="/about" hash="about">
              About
            </Link>
            <Link to="/about" hash="contact">
              Contact
            </Link>
            <Link to="/roadmap">Roadmap</Link>
            <Link to="/leaderboard" search={{ sortBy: 'xp', following: false }}>
              Leaderboard
            </Link>
            {onOpenKeymaps && (
              <button
                type="button"
                onClick={onOpenKeymaps}
                className="inline-flex items-center gap-1.5 hover:underline"
              >
                <Keyboard className="size-4" />
                Keymaps
              </button>
            )}
          </div>
          <div className="flex items-center">
            <a
              href="https://github.com/gauthier-se/checkpoint"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors font-medium underline underline-offset-4"
            >
              <Github className="size-5" />
              Open source on GitHub
            </a>
          </div>
        </div>
        <div className="mt-4 flex flex-col gap-1 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
          <p>Checkpoint is proudly made in Strasbourg, France</p>
          <p>
            © {new Date().getFullYear()} Checkpoint ·{' '}
            <Link
              to="/legal"
              hash="terms"
              className="underline underline-offset-4 transition-colors hover:text-foreground"
            >
              Terms of Service
            </Link>{' '}
            ·{' '}
            <Link
              to="/legal"
              hash="privacy"
              className="underline underline-offset-4 transition-colors hover:text-foreground"
            >
              Privacy Policy
            </Link>
          </p>
        </div>
      </div>
    </footer>
  )
}
