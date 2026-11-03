import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { collectionApi, type TemplateRequest } from '../api/collection';
import { unwrap } from '../api/problem';

export const collectionKeys = {
  all: ['collections'] as const,
  list: () => [...collectionKeys.all, 'list'] as const,
  detail: (id: number) => [...collectionKeys.all, 'detail', id] as const,
};

/** Every collection, ordered by name, without its templates. */
export function useCollections() {
  return useQuery({
    queryKey: collectionKeys.list(),
    queryFn: async () => unwrap(await collectionApi.GET('/api/v1/collections')),
  });
}

/** One collection with its templates, in order; disabled while no collection is chosen. */
export function useCollection(id: number | undefined) {
  return useQuery({
    queryKey: collectionKeys.detail(id ?? 0),
    queryFn: async () =>
      unwrap(
        await collectionApi.GET('/api/v1/collections/{collectionId}', {
          params: { path: { collectionId: id ?? 0 } },
        }),
      ),
    enabled: id !== undefined,
  });
}

/** Invalidates every collection query: lists show template counts, so any change affects them. */
export function useInvalidateCollections() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: collectionKeys.all });
}

export function useCreateCollection() {
  const invalidate = useInvalidateCollections();
  return useMutation({
    mutationFn: async (name: string) =>
      unwrap(await collectionApi.POST('/api/v1/collections', { body: { name } })),
    onSuccess: invalidate,
  });
}

export function useAddTemplate() {
  const invalidate = useInvalidateCollections();
  return useMutation({
    mutationFn: async ({
      collectionId,
      request,
    }: {
      collectionId: number;
      request: TemplateRequest;
    }) =>
      unwrap(
        await collectionApi.POST('/api/v1/collections/{collectionId}/templates', {
          params: { path: { collectionId } },
          body: request,
        }),
      ),
    onSuccess: invalidate,
  });
}

export function useReplaceTemplate() {
  const invalidate = useInvalidateCollections();
  return useMutation({
    mutationFn: async ({
      collectionId,
      templateId,
      request,
    }: {
      collectionId: number;
      templateId: number;
      request: TemplateRequest;
    }) =>
      unwrap(
        await collectionApi.PUT('/api/v1/collections/{collectionId}/templates/{templateId}', {
          params: { path: { collectionId, templateId } },
          body: request,
        }),
      ),
    onSuccess: invalidate,
  });
}
