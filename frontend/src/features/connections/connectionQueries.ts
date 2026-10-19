import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  connectionApi,
  type CreateConnectionRequest,
  type UpdateConnectionRequest,
} from '../../api/connection';
import { unwrap } from '../../api/problem';
import { connectionKeys } from '../../shared/connections';

function useInvalidateConnections() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: connectionKeys.all });
}

export function useCreateConnection() {
  const invalidate = useInvalidateConnections();
  return useMutation({
    mutationFn: async (request: CreateConnectionRequest) =>
      unwrap(await connectionApi.POST('/api/v1/connections', { body: request })),
    onSuccess: invalidate,
  });
}

export function useUpdateConnection() {
  const invalidate = useInvalidateConnections();
  return useMutation({
    mutationFn: async ({ id, request }: { id: string; request: UpdateConnectionRequest }) =>
      unwrap(
        await connectionApi.PATCH('/api/v1/connections/{connectionId}', {
          params: { path: { connectionId: id } },
          body: request,
        }),
      ),
    onSuccess: invalidate,
  });
}

export function useDeleteConnection() {
  const invalidate = useInvalidateConnections();
  return useMutation({
    mutationFn: async (id: string) => {
      unwrap(
        await connectionApi.DELETE('/api/v1/connections/{connectionId}', {
          params: { path: { connectionId: id } },
        }),
      );
    },
    onSuccess: invalidate,
  });
}
