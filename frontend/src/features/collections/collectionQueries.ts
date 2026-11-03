import { useMutation } from '@tanstack/react-query';
import { collectionApi } from '../../api/collection';
import { unwrap } from '../../api/problem';
import { useInvalidateCollections } from '../../shared/collections';

export function useRenameCollection() {
  const invalidate = useInvalidateCollections();
  return useMutation({
    mutationFn: async ({ id, name }: { id: number; name: string }) =>
      unwrap(
        await collectionApi.PATCH('/api/v1/collections/{collectionId}', {
          params: { path: { collectionId: id } },
          body: { name },
        }),
      ),
    onSuccess: invalidate,
  });
}

export function useDeleteCollection() {
  const invalidate = useInvalidateCollections();
  return useMutation({
    mutationFn: async (id: number) => {
      unwrap(
        await collectionApi.DELETE('/api/v1/collections/{collectionId}', {
          params: { path: { collectionId: id } },
        }),
      );
    },
    onSuccess: invalidate,
  });
}

export function useDeleteTemplate() {
  const invalidate = useInvalidateCollections();
  return useMutation({
    mutationFn: async ({
      collectionId,
      templateId,
    }: {
      collectionId: number;
      templateId: number;
    }) => {
      unwrap(
        await collectionApi.DELETE('/api/v1/collections/{collectionId}/templates/{templateId}', {
          params: { path: { collectionId, templateId } },
        }),
      );
    },
    onSuccess: invalidate,
  });
}

/** Resolves a template's placeholders; nothing is produced. Each call generates fresh built-ins. */
export function useResolveTemplate() {
  return useMutation({
    mutationFn: async ({
      collectionId,
      templateId,
      environmentId,
    }: {
      collectionId: number;
      templateId: number;
      environmentId: number | undefined;
    }) =>
      unwrap(
        await collectionApi.POST(
          '/api/v1/collections/{collectionId}/templates/{templateId}/resolution',
          {
            params: { path: { collectionId, templateId } },
            body: environmentId === undefined ? {} : { environmentId },
          },
        ),
      ),
  });
}
