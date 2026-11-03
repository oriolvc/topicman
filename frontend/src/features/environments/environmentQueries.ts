import { useMutation, useQueryClient } from '@tanstack/react-query';
import { collectionApi, type EnvironmentRequest } from '../../api/collection';
import { unwrap } from '../../api/problem';
import { environmentKeys } from '../../shared/environments';

function useInvalidateEnvironments() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: environmentKeys.all });
}

export function useCreateEnvironment() {
  const invalidate = useInvalidateEnvironments();
  return useMutation({
    mutationFn: async (request: EnvironmentRequest) =>
      unwrap(await collectionApi.POST('/api/v1/environments', { body: request })),
    onSuccess: invalidate,
  });
}

export function useReplaceEnvironment() {
  const invalidate = useInvalidateEnvironments();
  return useMutation({
    mutationFn: async ({ id, request }: { id: number; request: EnvironmentRequest }) =>
      unwrap(
        await collectionApi.PUT('/api/v1/environments/{environmentId}', {
          params: { path: { environmentId: id } },
          body: request,
        }),
      ),
    onSuccess: invalidate,
  });
}

export function useDeleteEnvironment() {
  const invalidate = useInvalidateEnvironments();
  return useMutation({
    mutationFn: async (id: number) => {
      unwrap(
        await collectionApi.DELETE('/api/v1/environments/{environmentId}', {
          params: { path: { environmentId: id } },
        }),
      );
    },
    onSuccess: invalidate,
  });
}
