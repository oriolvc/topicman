import { useQuery } from '@tanstack/react-query';
import { connectionApi } from '../api/connection';
import { unwrap } from '../api/problem';

export const connectionKeys = {
  all: ['connections'] as const,
  list: () => [...connectionKeys.all, 'list'] as const,
};

/** The saved connections, ordered by name; every screen that picks a connection shares this cache. */
export function useConnections() {
  return useQuery({
    queryKey: connectionKeys.list(),
    queryFn: async () => unwrap(await connectionApi.GET('/api/v1/connections')),
  });
}
