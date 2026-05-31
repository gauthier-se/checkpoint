import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { GameInteractionStatusDto } from '@/types/interaction'
import { gameInteractionStatusQueryOptions } from '@/queries/games'

interface GameInteractionMutationConfig<TVariables> {
  gameId: string
  /** Performs the server-side mutation. */
  mutationFn: (variables: TVariables) => Promise<unknown>
  /** Returns the optimistically-updated status from the current cached one. */
  optimisticPatch: (
    current: GameInteractionStatusDto,
    variables: TVariables,
  ) => GameInteractionStatusDto
  /** Toast shown on failure, after the optimistic change is rolled back. */
  errorMessage: string
  /** Optional success toast — a static string or a factory from the inputs. */
  successMessage?:
    | string
    | ((
        variables: TVariables,
        previous: GameInteractionStatusDto | undefined,
      ) => string)
}

/**
 * Wraps the optimistic-update boilerplate shared by every mutation that edits
 * the cached {@link GameInteractionStatusDto}: cancel in-flight queries,
 * snapshot the previous value, apply an optimistic patch, roll back (and toast)
 * on error, and revalidate on settle. The global error handler is suppressed so
 * each action can surface its own message.
 */
export function useGameInteractionMutation<TVariables = void>({
  gameId,
  mutationFn,
  optimisticPatch,
  errorMessage,
  successMessage,
}: GameInteractionMutationConfig<TVariables>) {
  const queryClient = useQueryClient()
  const options = gameInteractionStatusQueryOptions(gameId)

  return useMutation({
    meta: { suppressGlobalError: true },
    mutationFn,
    onMutate: async (variables) => {
      await queryClient.cancelQueries(options)
      const previous = queryClient.getQueryData<GameInteractionStatusDto>(
        options.queryKey,
      )
      queryClient.setQueryData<GameInteractionStatusDto>(
        options.queryKey,
        (old) => (old ? optimisticPatch(old, variables) : old),
      )
      return { previous }
    },
    onError: (_err, _variables, context) => {
      toast.error(errorMessage)
      if (context?.previous) {
        queryClient.setQueryData(options.queryKey, context.previous)
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries(options)
    },
    onSuccess: successMessage
      ? (_data, variables, context) => {
          toast.success(
            typeof successMessage === 'function'
              ? successMessage(variables, context.previous)
              : successMessage,
          )
        }
      : undefined,
  })
}
