import { QueryClient } from '@tanstack/react-query';
import { ApiProblem } from '../api/problem';

const MAX_RETRIES = 2;

/** Retries only failures that may be transient: never a client error such as a 404. */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: (failureCount, error) =>
          failureCount < MAX_RETRIES && !(error instanceof ApiProblem && error.status < 500),
      },
    },
  });
}
