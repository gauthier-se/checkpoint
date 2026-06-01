import { useEffect, useState } from 'react'
import { Link } from '@tanstack/react-router'
import { useHotkey, useHotkeySequence } from '@tanstack/react-hotkeys'
import { Menu, Plus, Search, User } from 'lucide-react'
import { Button } from '../ui/button'
import { KbdHint } from '../ui/kbd'
import { Separator } from '../ui/separator'
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '../ui/sheet'
import { Tooltip, TooltipContent, TooltipTrigger } from '../ui/tooltip'
import { SearchCommand } from '../search/search-command'
import { QuickLogModal } from '../log/quick-log-modal'
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

const NAV_LINKS = [
  { to: '/games' as const, search: { page: 1 }, label: 'Games' },
  { to: '/lists' as const, search: { page: 1 }, label: 'Lists' },
  { to: '/members' as const, search: undefined, label: 'Members' },
  { to: '/news' as const, search: { page: 1 }, label: 'News' },
]

export const Header = () => {
  const { user, isLoading } = useAuth()
  const [searchOpen, setSearchOpen] = useState(false)
  const [quickLogOpen, setQuickLogOpen] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
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

  function closeMobile() {
    setMobileMenuOpen(false)
  }

  return (
    <header className="relative z-20 w-full">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4">
        <Link className="flex items-center gap-2" to="/">
          <img className="w-8" src="/images/logo.png" alt="" />
          <h1 className="text-2xl font-bold">Checkpoint</h1>
        </Link>

        {/* Desktop nav */}
        <nav className="hidden items-center gap-4 md:flex">
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
              <KbdHint
                keys={['Mod', 'K']}
                isMac={isMac}
                className="hidden sm:inline-flex"
              />
            )}
          </button>
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

        {/* Mobile hamburger */}
        <button
          className="flex items-center justify-center rounded-md p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground md:hidden"
          onClick={() => setMobileMenuOpen(true)}
          aria-label="Open menu"
        >
          <Menu className="size-5" />
        </button>
      </div>

      {/* Mobile nav sheet */}
      <Sheet open={mobileMenuOpen} onOpenChange={setMobileMenuOpen}>
        <SheetContent side="left" className="flex flex-col gap-0 p-0">
          <SheetHeader className="px-4 pt-4 pb-2">
            <SheetTitle asChild>
              <Link
                className="flex items-center gap-2"
                to="/"
                onClick={closeMobile}
              >
                <img className="w-7" src="/images/logo.png" alt="" />
                <span className="text-xl font-bold">Checkpoint</span>
              </Link>
            </SheetTitle>
          </SheetHeader>

          <Separator />

          <nav className="flex flex-col gap-1 px-2 py-3">
            {NAV_LINKS.map(({ to, search, label }) => (
              <Link
                key={to}
                to={to}
                search={search as Record<string, unknown>}
                onClick={closeMobile}
                className="rounded-md px-3 py-2.5 text-sm font-semibold text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                activeProps={{ className: 'bg-muted text-foreground' }}
              >
                {label}
              </Link>
            ))}
          </nav>

          <Separator />

          <div className="px-2 py-3">
            <button
              onClick={() => {
                closeMobile()
                setSearchOpen(true)
              }}
              className="flex w-full items-center gap-2 rounded-md px-3 py-2.5 text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            >
              <Search className="size-4 shrink-0" />
              <span>Search...</span>
            </button>
          </div>

          <Separator />

          <div className="px-4 py-4">
            {isLoading ? (
              <div className="h-9 w-full animate-pulse rounded-md bg-muted" />
            ) : user ? (
              <div className="flex items-center gap-3">
                <Button
                  size="sm"
                  className="flex-1"
                  onClick={() => {
                    closeMobile()
                    setQuickLogOpen(true)
                  }}
                >
                  <Plus />
                  Log a game
                </Button>
                <NotificationBell />
                <AvatarDropdown user={user} />
              </div>
            ) : (
              <Button asChild size="sm" className="w-full">
                <Link to="/login" onClick={closeMobile}>
                  <User />
                  Sign in
                </Link>
              </Button>
            )}
          </div>
        </SheetContent>
      </Sheet>

      <SearchCommand open={searchOpen} onOpenChange={setSearchOpen} />
      <QuickLogModal open={quickLogOpen} onOpenChange={setQuickLogOpen} />
    </header>
  )
}
