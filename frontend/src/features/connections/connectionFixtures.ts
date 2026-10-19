import type { ConnectionResponse } from '../../api/connection';

export function aConnection(overrides: Partial<ConnectionResponse> = {}): ConnectionResponse {
  return {
    id: '7f0c3c1e-5b8e-4a55-9a43-0b6c1f1d2a10',
    name: 'Local Kafka',
    brokerType: 'KAFKA',
    address: 'localhost:9092',
    credentialsType: 'NONE',
    createdAt: '2026-10-18T10:00:00Z',
    updatedAt: '2026-10-18T10:00:00Z',
    ...overrides,
  };
}
