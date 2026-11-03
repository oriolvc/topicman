import type { EnvironmentResponse } from '../api/collection';

export function anEnvironment(overrides: Partial<EnvironmentResponse> = {}): EnvironmentResponse {
  return {
    id: 1,
    name: 'staging',
    variables: [{ name: 'env', value: 'stg' }],
    createdAt: '2026-11-03T10:00:00Z',
    updatedAt: '2026-11-03T10:00:00Z',
    ...overrides,
  };
}
