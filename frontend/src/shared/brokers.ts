import { useQuery } from '@tanstack/react-query';
import type { BrokerType } from '../api/connection';
import { messagingApi } from '../api/messaging';
import { unwrap } from '../api/problem';

export const BROKER_TYPES: BrokerType[] = ['KAFKA', 'PULSAR', 'RABBITMQ'];

export const BROKER_LABELS: Record<BrokerType, string> = {
  KAFKA: 'Kafka',
  PULSAR: 'Pulsar',
  RABBITMQ: 'RabbitMQ',
};

export const brokerKeys = {
  all: ['brokers'] as const,
};

/**
 * The brokers the server supports, with their capabilities (destination kind, message keys,
 * reading from the beginning). The list is fixed while the server runs, so it is never refetched.
 */
export function useBrokers() {
  return useQuery({
    queryKey: brokerKeys.all,
    queryFn: async () => unwrap(await messagingApi.GET('/api/v1/brokers')),
    staleTime: Infinity,
  });
}
