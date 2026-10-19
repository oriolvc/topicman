import { useMutation } from '@tanstack/react-query';
import { messagingApi, type OpenConsumeSessionRequest } from '../../api/messaging';
import { unwrap } from '../../api/problem';

export function useOpenConsumeSession() {
  return useMutation({
    mutationFn: async (request: OpenConsumeSessionRequest) =>
      unwrap(await messagingApi.POST('/api/v1/consume-sessions', { body: request })),
  });
}

/**
 * Releases a session's broker client. Best effort: the session may already be closed, and one left
 * open expires once idle.
 */
export function closeConsumeSession(sessionId: string): void {
  messagingApi
    .DELETE('/api/v1/consume-sessions/{sessionId}', { params: { path: { sessionId } } })
    .catch(() => undefined);
}
