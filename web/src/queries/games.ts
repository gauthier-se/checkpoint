import { queryOptions } from '@tanstack/react-query'
import type { Priority } from '@/types/collection'
import type {
  GameInteractionStatusDto,
  GamePlayLogRequestDto,
  GamePlayLogResponseDto,
  RateResponseDto,
} from '@/types/interaction'
import type { UserGameRequest } from '@/types/library'
import { apiFetch } from '@/services/api'

export function gameInteractionStatusQueryOptions(gameId: string) {
  return queryOptions({
    queryKey: ['games', gameId, 'interaction-status'],
    queryFn: async () => {
      const res = await apiFetch(`/api/me/games/${gameId}/status`)
      if (!res.ok) {
        throw new Error('Failed to fetch interaction status')
      }
      return res.json() as Promise<GameInteractionStatusDto>
    },
  })
}

export function userRatingQueryOptions(gameId: string) {
  return queryOptions({
    queryKey: ['games', gameId, 'rate'],
    queryFn: async () => {
      const res = await apiFetch(`/api/me/games/${gameId}/rate`)
      if (res.status === 404) {
        return null
      }
      if (!res.ok) {
        throw new Error('Failed to fetch user rating')
      }
      return res.json() as Promise<RateResponseDto>
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
  if (!res.ok) {
    throw new Error('Failed to rate game')
  }
  return res.json() as Promise<RateResponseDto>
}

export async function removeRating(gameId: string) {
  const res = await apiFetch(`/api/me/games/${gameId}/rate`, {
    method: 'DELETE',
  })
  if (!res.ok && res.status !== 204) {
    throw new Error('Failed to remove rating')
  }
}

export async function toggleWishlist(
  gameId: string,
  currentStatus: boolean,
  priority: Priority | null = null,
) {
  if (currentStatus) {
    const res = await apiFetch(`/api/me/wishlist/${gameId}`, {
      method: 'DELETE',
    })
    if (!res.ok && res.status !== 204) {
      throw new Error('Failed to toggle wishlist')
    }
    return
  }
  const res = await apiFetch(`/api/me/wishlist/${gameId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ priority }),
  })
  if (!res.ok) {
    throw new Error('Failed to toggle wishlist')
  }
}

export async function toggleBacklog(
  gameId: string,
  currentStatus: boolean,
  priority: Priority | null = null,
) {
  if (currentStatus) {
    const res = await apiFetch(`/api/me/backlog/${gameId}`, {
      method: 'DELETE',
    })
    if (!res.ok && res.status !== 204) {
      throw new Error('Failed to toggle backlog')
    }
    return
  }
  const res = await apiFetch(`/api/me/backlog/${gameId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ priority }),
  })
  if (!res.ok) {
    throw new Error('Failed to toggle backlog')
  }
}

export async function updateWishlistPriority(
  gameId: string,
  priority: Priority | null,
) {
  const res = await apiFetch(`/api/me/wishlist/${gameId}/priority`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ priority }),
  })
  if (!res.ok) {
    throw new Error('Failed to update wishlist priority')
  }
}

export async function updateBacklogPriority(
  gameId: string,
  priority: Priority | null,
) {
  const res = await apiFetch(`/api/me/backlog/${gameId}/priority`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ priority }),
  })
  if (!res.ok) {
    throw new Error('Failed to update backlog priority')
  }
}

export async function updateLibraryStatus(
  gameId: string,
  request: UserGameRequest | null,
) {
  if (!request) {
    const res = await apiFetch(`/api/me/library/${gameId}`, {
      method: 'DELETE',
    })
    if (!res.ok && res.status !== 204) {
      throw new Error('Failed to remove from library')
    }
  } else {
    let res = await apiFetch(`/api/me/library/${gameId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })

    // If it doesn't exist, POST to create it
    if (res.status === 404) {
      res = await apiFetch('/api/me/library', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      })
    }

    if (!res.ok) {
      throw new Error('Failed to update library')
    }
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
  if (!res.ok) {
    throw new Error('Failed to log play')
  }
  return res.json() as Promise<GamePlayLogResponseDto>
}
