import { useEffect, useState } from 'react'

const DESKTOP_QUERY = '(min-width: 640px)'

export function useIsDesktop(): boolean {
  const [isDesktop, setIsDesktop] = useState(false)

  useEffect(() => {
    const mql = window.matchMedia(DESKTOP_QUERY)
    setIsDesktop(mql.matches)

    const handler = (event: MediaQueryListEvent) => setIsDesktop(event.matches)
    mql.addEventListener('change', handler)
    return () => mql.removeEventListener('change', handler)
  }, [])

  return isDesktop
}
