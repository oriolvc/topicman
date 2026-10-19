import { useQuery } from '@tanstack/react-query';
import { connectionApi, type ConnectionResponse } from '../api/connection';
import { unwrap } from '../api/problem';
import { BROKER_LABELS } from './brokers';

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

/** The options of a connection picker: each connection's name and broker. */
export function connectionOptions(connections: ConnectionResponse[]) {
  return connections.map((connection) => ({
    value: connection.id,
    label: `${connection.name} (${BROKER_LABELS[connection.brokerType]})`,
  }));
}
