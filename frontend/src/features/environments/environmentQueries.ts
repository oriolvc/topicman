import { useMutation, useQueryClient } from '@tanstack/react-query';
import { collectionApi, type EnvironmentFile, type EnvironmentRequest } from '../../api/collection';
import { unwrap } from '../../api/problem';
import { environmentKeys } from '../../shared/environments';
import { fileNameOf, saveFile, yamlBody } from '../../shared/files';

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

/** Creates an environment from a YAML file's text, under another name when `name` is given. */
export function useImportEnvironment() {
  const invalidate = useInvalidateEnvironments();
  return useMutation({
    mutationFn: async ({ text, name }: { text: string; name?: string }) =>
      unwrap(
        await collectionApi.POST('/api/v1/environments/import', {
          params: { query: name === undefined ? {} : { name } },
          ...yamlBody<EnvironmentFile>(text),
        }),
      ),
    onSuccess: invalidate,
  });
}

/** Downloads an environment as the YAML file the server writes. */
export function useExportEnvironment() {
  return useMutation({
    mutationFn: async ({ id, name }: { id: number; name: string }) => {
      const result = await collectionApi.GET('/api/v1/environments/{environmentId}/export', {
        params: { path: { environmentId: id } },
        parseAs: 'text',
      });
      saveFile(fileNameOf(result.response, `${name}.topicman.yaml`), unwrap(result));
    },
  });
}
