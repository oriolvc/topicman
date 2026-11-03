import { useLocalStorage } from '@mantine/hooks';
import { useQuery } from '@tanstack/react-query';
import { collectionApi } from '../api/collection';
import { unwrap } from '../api/problem';

export const environmentKeys = {
  all: ['environments'] as const,
};

/** Every environment with its variables, ordered by name. */
export function useEnvironments() {
  return useQuery({
    queryKey: environmentKeys.all,
    queryFn: async () => unwrap(await collectionApi.GET('/api/v1/environments')),
  });
}

const ACTIVE_ENVIRONMENT_KEY = 'topicman.activeEnvironmentId';

/**
 * The environment templates are resolved against, chosen in the header and kept in this browser.
 * An environment that no longer exists counts as none.
 */
export function useActiveEnvironment() {
  const environments = useEnvironments();
  const [stored, setStored] = useLocalStorage<string | null>({
    key: ACTIVE_ENVIRONMENT_KEY,
    defaultValue: null,
    getInitialValueInEffect: false,
  });
  const environment = environments.data?.find(({ id }) => String(id) === stored);
  const choose = (environmentId: number | null) => {
    setStored(environmentId === null ? null : String(environmentId));
  };
  return { environment, choose, environments };
}
