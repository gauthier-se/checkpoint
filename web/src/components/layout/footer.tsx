import { Link } from '@tanstack/react-router'
import { Github, Keyboard } from 'lucide-react'

interface FooterProps {
  onOpenKeymaps?: () => void
}

export const Footer = ({ onOpenKeymaps }: FooterProps) => {
  return (
    <footer className="w-full bg-muted">
      <div className="max-w-7xl mx-auto py-10">
        <div className="flex justify-between items-center">
          <div className="flex items-center font-semibold gap-4">
            <Link to="/about" hash="about">
              About
            </Link>
            <Link to="/about" hash="contact">
              Contact
            </Link>
            <Link to="/roadmap">Roadmap</Link>
            <Link to="/leaderboard" search={{ sortBy: 'xp' }}>
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
        <div className="flex items-center justify-between mt-4 text-sm text-muted-foreground">
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
