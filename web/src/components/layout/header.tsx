import { useEffect, useState } from 'react'
import { Link } from '@tanstack/react-router'
import { useHotkey, useHotkeySequence } from '@tanstack/react-hotkeys'
import { Command, Plus, Search, User } from 'lucide-react'
import { Button } from '../ui/button'
import { SearchCommand } from '../search/search-command'
import { QuickLogModal } from '../log/quick-log-modal'
import { Tooltip, TooltipContent, TooltipTrigger } from '../ui/tooltip'
import { KbdHint } from '../ui/kbd'
import { AvatarDropdown } from './avatar-dropdown'
import { NotificationBell } from './notification-bell'
import { useAuth } from '@/hooks/use-auth'
import { useNavigationHotkeys } from '@/hooks/use-navigation-hotkeys'

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

  useHotkeySequence(
    ['L', 'G'],
    () => {
      setQuickLogOpen(true)
    },
    { enabled: !!user },
  )

  useNavigationHotkeys()

  return (
    <header className="relative z-20 max-w-7xl mx-auto py-4 flex items-center justify-between">
      <Link className="flex items-center gap-2" to="/">
        <img className="w-8 pt-1" src="/images/logo.png" alt="" />
        <h1 className="text-2xl font-bold">Checkpoint</h1>
      </Link>
      <nav className="flex items-center gap-4 pt-2">
        <Tooltip>
          <TooltipTrigger asChild>
            <Link
              to="/games"
              search={{ page: 1 }}
              className="text-muted-foreground font-semibold"
            >
              Games
            </Link>
          </TooltipTrigger>
          <TooltipContent className="flex items-center gap-2">
            <span>Games</span>
            <KbdHint keys={['G', 'G']} />
          </TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Link
              to="/lists"
              search={{ page: 1 }}
              className="text-muted-foreground font-semibold"
            >
              Lists
            </Link>
          </TooltipTrigger>
          <TooltipContent className="flex items-center gap-2">
            <span>Lists</span>
            <KbdHint keys={['G', 'L']} />
          </TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Link
              to="/members"
              search={{ page: 1 }}
              className="text-muted-foreground font-semibold"
            >
              Members
            </Link>
          </TooltipTrigger>
          <TooltipContent className="flex items-center gap-2">
            <span>Members</span>
            <KbdHint keys={['G', 'M']} />
          </TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Link
              to="/news"
              search={{ page: 1 }}
              className="text-muted-foreground font-semibold"
            >
              News
            </Link>
          </TooltipTrigger>
          <TooltipContent className="flex items-center gap-2">
            <span>News</span>
            <KbdHint keys={['G', 'W']} />
          </TooltipContent>
        </Tooltip>
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
        {isLoading ? (
          <div className="h-9 w-32 animate-pulse rounded-md bg-muted" />
        ) : user ? (
          <>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button size="sm" onClick={() => setQuickLogOpen(true)}>
                  <Plus />
                  Log
                </Button>
              </TooltipTrigger>
              <TooltipContent className="flex items-center gap-2">
                <span>Quick log</span>
                <KbdHint keys={['L', 'G']} isMac={isMac} />
              </TooltipContent>
            </Tooltip>
            <QuickLogModal open={quickLogOpen} onOpenChange={setQuickLogOpen} />
            <NotificationBell />
            <AvatarDropdown user={user} />
          </>
        ) : (
          <Button asChild size="sm">
            <Link to="/login">
              <User />
              Sign in
            </Link>
          </Button>
        )}
      </nav>
    </header>
  )
}
