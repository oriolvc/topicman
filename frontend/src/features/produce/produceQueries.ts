import { useMutation } from '@tanstack/react-query';
import { messagingApi, type ProduceMessageRequest } from '../../api/messaging';
import { unwrap } from '../../api/problem';

export function useProduceMessage() {
  return useMutation({
    mutationFn: async (request: ProduceMessageRequest) =>
      unwrap(await messagingApi.POST('/api/v1/messages', { body: request })),
  });
}
