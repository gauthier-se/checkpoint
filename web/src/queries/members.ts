import { keepPreviousData, queryOptions } from '@tanstack/react-query'
import type { MemberCard, MembersResponse } from '@/types/member'
import { apiFetch } from '@/services/api'

export function popularMembersQueryOptions(size: number = 10) {
  return queryOptions({
    queryKey: ['members', 'popular', size],
    queryFn: async (): Promise<Array<MemberCard>> => {
      const res = await apiFetch(`/api/members/popular?size=${size}`)
      return res.json()
    },
    staleTime: 0,
  })
}

export function topReviewersMembersQueryOptions(size: number = 10) {
  return queryOptions({
    queryKey: ['members', 'top-reviewers', size],
    queryFn: async (): Promise<Array<MemberCard>> => {
      const res = await apiFetch(`/api/members/top-reviewers?size=${size}`)
      return res.json()
    },
    staleTime: 0,
  })
}

export function suggestedMembersQueryOptions(size: number = 10) {
  return queryOptions({
    queryKey: ['members', 'suggested', size],
    queryFn: async (): Promise<Array<MemberCard>> => {
      const res = await apiFetch(`/api/members/suggested?size=${size}`)
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}

export function recentMembersQueryOptions(size: number = 10) {
  return queryOptions({
    queryKey: ['members', 'recent', size],
    queryFn: async (): Promise<Array<MemberCard>> => {
      const res = await apiFetch(`/api/members/recent?size=${size}`)
      return res.json()
    },
    staleTime: 0,
  })
}

export function searchMembersQueryOptions(query: string) {
  return queryOptions({
    queryKey: ['members', 'search', query],
    queryFn: async (): Promise<MembersResponse> => {
      const qs = new URLSearchParams()
      qs.set('page', '0')
      qs.set('size', '5')
      qs.set('search', query)
      const res = await apiFetch(`/api/members?${qs.toString()}`)
      return res.json()
    },
    staleTime: 30 * 1000,
    enabled: query.length >= 2,
    placeholderData: keepPreviousData,
  })
}

export function browseMembersQueryOptions(
  page: number = 0,
  size: number = 20,
  search?: string,
) {
  return queryOptions({
    queryKey: ['members', 'browse', search, page, size],
    queryFn: async (): Promise<MembersResponse> => {
      const qs = new URLSearchParams()
      qs.set('page', String(page))
      qs.set('size', String(size))
      if (search) qs.set('search', search)
      const res = await apiFetch(`/api/members?${qs.toString()}`)
      return res.json()
    },
    staleTime: 0,
  })
}
