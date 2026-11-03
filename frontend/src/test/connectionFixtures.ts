import type { ConnectionResponse } from '../api/connection';

export function aConnection(overrides: Partial<ConnectionResponse> = {}): ConnectionResponse {
  return {
    id: 1,
    name: 'Local Kafka',
    brokerType: 'KAFKA',
    address: 'localhost:9092',
    credentialsType: 'NONE',
    createdAt: '2026-10-18T10:00:00Z',
    updatedAt: '2026-10-18T10:00:00Z',
    ...overrides,
  };
}
