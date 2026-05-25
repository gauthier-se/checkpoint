import { queryOptions } from '@tanstack/react-query'
import type { Priority } from '@/types/collection'
import type {
  GameInteractionStatusDto,
  GamePlayLogRequestDto,
  GamePlayLogResponseDto,
  RateResponseDto,
} from '@/types/interaction'
import type { UserGameRequest } from '@/types/library'
import { apiFetch, isApiError } from '@/services/api'

export function gameInteractionStatusQueryOptions(gameId: string) {
  return queryOptions({
    queryKey: ['games', gameId, 'interaction-status'],
    queryFn: async () => {
      const res = await apiFetch(`/api/me/games/${gameId}/status`)
      return res.json() as Promise<GameInteractionStatusDto>
    },
  })
}

export function userRatingQueryOptions(gameId: string) {
  return queryOptions({
    queryKey: ['games', gameId, 'rate'],
    queryFn: async () => {
      try {
        const res = await apiFetch(`/api/me/games/${gameId}/rate`)
        return (await res.json()) as RateResponseDto
      } catch (e) {
        // 404 means the user hasn't rated this game yet — not an error.
        if (isApiError(e) && e.status === 404) return null
        throw e
      }
    },
  })
}

export async function rateGame(gameId: string, score: number) {
  const res = await apiFetch(`/api/me/games/${gameId}/rate`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ score }),
  })
  return res.json() as Promise<RateResponseDto>
}

export async function removeRating(gameId: string) {
  await apiFetch(`/api/me/games/${gameId}/rate`, {
    method: 'DELETE',
  })
}

export async function toggleWishlist(
  gameId: string,
  currentStatus: boolean,
  priority: Priority | null = null,
) {
  if (currentStatus) {
    await apiFetch(`/api/me/wishlist/${gameId}`, {
      method: 'DELETE',
    })
    return
  }
  await apiFetch(`/api/me/wishlist/${gameId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ priority }),
  })
}

export async function toggleBacklog(
  gameId: string,
  currentStatus: boolean,
  priority: Priority | null = null,
) {
  if (currentStatus) {
    await apiFetch(`/api/me/backlog/${gameId}`, {
      method: 'DELETE',
    })
    return
  }
  await apiFetch(`/api/me/backlog/${gameId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ priority }),
  })
}

export async function updateWishlistPriority(
  gameId: string,
  priority: Priority | null,
) {
  await apiFetch(`/api/me/wishlist/${gameId}/priority`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ priority }),
  })
}

export async function updateBacklogPriority(
  gameId: string,
  priority: Priority | null,
) {
  await apiFetch(`/api/me/backlog/${gameId}/priority`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ priority }),
  })
}

/**
 * Toggles a "like" on a game. A like marks a game the user loves — distinct from
 * the wishlist (games the user wants to buy). The endpoint is a server-side toggle:
 * a single POST likes or unlikes depending on the current state.
 */
export async function toggleGameLike(gameId: string) {
  const res = await apiFetch(`/api/me/games/${gameId}/like`, {
    method: 'POST',
  })
  return res.json() as Promise<{ liked: boolean; likesCount: number }>
}

export async function updateLibraryStatus(
  gameId: string,
  request: UserGameRequest | null,
) {
  if (!request) {
    await apiFetch(`/api/me/library/${gameId}`, {
      method: 'DELETE',
    })
    return
  }

  try {
    await apiFetch(`/api/me/library/${gameId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })
  } catch (e) {
    // If the library entry doesn't exist yet, fall back to POST to create it.
    if (isApiError(e) && e.status === 404) {
      await apiFetch('/api/me/library', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      })
      return
    }
    throw e
  }
}

export async function logPlay(request: GamePlayLogRequestDto) {
  const res = await apiFetch('/api/me/plays', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
  return res.json() as Promise<GamePlayLogResponseDto>
}
