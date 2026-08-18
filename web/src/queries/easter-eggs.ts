import { apiFetch } from '@/services/api'

async function trigger(path: string): Promise<void> {
  try {
    await apiFetch(`/api/me/easter-eggs/${path}`, { method: 'POST' })
  } catch {
    // Easter-egg badges are fire-and-forget: an offline user or a 401 should
    // not surface anything to the UI. The badge stays "undiscovered" and the
    // user can trigger it again later.
  }
}

export const triggerKonami = () => trigger('konami')
export const triggerBarrelRoll = () => trigger('barrel-roll')
export const triggerRickroll = () => trigger('rickroll')
export const triggerNaviClick = () => trigger('navi-clicks')

export const triggerReviewView = (reviewId: string) =>
  trigger(`review-view/${reviewId}`)
