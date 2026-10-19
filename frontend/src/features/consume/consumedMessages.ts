import type { ConsumedMessageEvent, Payload } from '../../api/messaging';

/** How many messages the screen keeps; older ones are dropped so a busy topic cannot exhaust memory. */
export const MAX_MESSAGES = 500;

export type ConsumedMessage = ConsumedMessageEvent & {
  /** Stable React key, in arrival order; positions are not unique across brokers. */
  seq: number;
};

/** Newest first, keeping at most {@link MAX_MESSAGES}. */
export function addMessage(
  messages: ConsumedMessage[],
  message: ConsumedMessage,
): ConsumedMessage[] {
  return [message, ...messages.slice(0, MAX_MESSAGES - 1)];
}

/** Pretty-prints JSON payloads; anything else, or `raw`, is shown as received. */
export function formatPayload(payload: Payload, raw: boolean): string {
  if (raw || payload.format !== 'JSON') {
    return payload.content;
  }
  try {
    return JSON.stringify(JSON.parse(payload.content), null, 2);
  } catch {
    return payload.content;
  }
}
