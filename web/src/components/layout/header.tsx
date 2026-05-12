import { useEffect, useState } from 'react'
import { Link } from '@tanstack/react-router'
import { useHotkey } from '@tanstack/react-hotkeys'
import { Command, Plus, Search, User } from 'lucide-react'
import { Button } from '../ui/button'
import { SearchCommand } from '../search/search-command'
import { QuickLogModal } from '../log/quick-log-modal'
import { AvatarDropdown } from './avatar-dropdown'
import { NotificationBell } from './notification-bell'
import { useAuth } from '@/hooks/use-auth'

function useIsMac() {
  const [isMac, setIsMac] = useState<boolean | null>(null)
  useEffect(() => {
    setIsMac(/Mac|iPod|iPhone|iPad/.test(navigator.userAgent))
  }, [])
  return isMac
}

export const Header = () => {
  const { user, isLoading } = useAuth()
  const [searchOpen, setSearchOpen] = useState(false)
  const [quickLogOpen, setQuickLogOpen] = useState(false)
  const isMac = useIsMac()

  useHotkey('Mod+K', () => {
    setSearchOpen((prev) => !prev)
  })

  useHotkey('Mod+J', () => {
    if (user) setQuickLogOpen(true)
  })

  return (
    <header className="relative z-20 max-w-7xl mx-auto py-4 flex items-center justify-between">
      <Link className="flex items-center gap-2" to="/">
        <img className="w-8 pt-1" src="/images/logo.png" alt="" />
        <h1 className="text-2xl font-bold">Checkpoint</h1>
      </Link>
      <nav className="flex items-center gap-4 pt-2">
        <Link
          to="/games"
          search={{ page: 1 }}
          className="text-muted-foreground font-semibold"
        >
          Games
        </Link>
        <Link
          to="/lists"
          search={{ page: 1 }}
          className="text-muted-foreground font-semibold"
        >
          Lists
        </Link>
        <Link
          to="/members"
          search={{ page: 1 }}
          className="text-muted-foreground font-semibold"
        >
          Members
        </Link>
        <Link
          to="/news"
          search={{ page: 1 }}
          className="text-muted-foreground font-semibold"
        >
          News
        </Link>
        <button
          onClick={() => setSearchOpen(true)}
          className="flex h-9 items-center gap-2 rounded-lg border bg-muted/50 px-3 text-sm text-muted-foreground transition-colors hover:bg-muted"
        >
          <Search className="size-4 shrink-0" />
          <span>Search...</span>
          {isMac !== null && (
            <kbd className="pointer-events-none hidden select-none items-center gap-0.5 rounded bg-muted px-1.5 py-0.5 font-mono text-[11px] font-medium text-muted-foreground sm:inline-flex">
              {isMac ? <Command className="size-2.5" /> : <span>Ctrl+</span>}
              <span>K</span>
            </kbd>
          )}
        </button>
        <SearchCommand open={searchOpen} onOpenChange={setSearchOpen} />
        {user && (
          <>
            <Button size="sm" onClick={() => setQuickLogOpen(true)}>
              <Plus />
              Log
            </Button>
            <QuickLogModal open={quickLogOpen} onOpenChange={setQuickLogOpen} />
            <NotificationBell />
          </>
        )}
        {!isLoading &&
          (user ? (
            <AvatarDropdown user={user} />
          ) : (
            <Button asChild size="sm">
              <Link to="/login">
                <User />
                Sign in
              </Link>
            </Button>
          ))}
      </nav>
    </header>
  )
}
