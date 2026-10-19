import { useEffect, useEffectEvent, useState } from 'react';
import type { ConsumedMessageEvent } from '../../api/messaging';
import type { ProblemDetail } from '../../api/problem';

export type StreamStatus =
  | { kind: 'connecting' }
  | { kind: 'streaming' }
  /** The connection dropped; `EventSource` retries on its own. */
  | { kind: 'reconnecting' }
  /** The server closed the session, such as after its idle timeout. */
  | { kind: 'closed' }
  /** Another window took the stream over; the session is still open. */
  | { kind: 'replaced' }
  /** The stream was refused or could not be retried, as for a session that no longer exists. */
  | { kind: 'lost' }
  | { kind: 'failed'; problem: ProblemDetail };

/** A connection to a session's event stream; a new `attempt` reconnects to the same session. */
export type StreamTarget = { eventsUrl: string; attempt: number };

/**
 * Streams a consume session's events (messaging.yaml) while `target` is set. The stream is closed
 * on every final event, so `EventSource` never reconnects to a session that ended or moved to
 * another window.
 */
export function useConsumeStream(
  target: StreamTarget | undefined,
  onMessage: (message: ConsumedMessageEvent) => void,
): StreamStatus | undefined {
  const [status, setStatus] = useState<StreamStatus>({ kind: 'connecting' });
  const deliver = useEffectEvent(onMessage);
  const eventsUrl = target?.eventsUrl;
  const attempt = target?.attempt;

  useEffect(() => {
    if (eventsUrl === undefined) {
      return;
    }
    const source = new EventSource(new URL(eventsUrl, window.location.origin));
    const end = (final: StreamStatus) => {
      source.close();
      setStatus(final);
    };
    source.addEventListener('open', () => {
      setStatus({ kind: 'streaming' });
    });
    source.addEventListener('message', (event: MessageEvent<string>) => {
      deliver(JSON.parse(event.data) as ConsumedMessageEvent);
    });
    source.addEventListener('failed', (event: MessageEvent<string>) => {
      end({ kind: 'failed', problem: parseProblem(event.data) });
    });
    source.addEventListener('closed', () => {
      end({ kind: 'closed' });
    });
    source.addEventListener('replaced', () => {
      end({ kind: 'replaced' });
    });
    // Only connection errors: the stream reports read failures as `failed` events.
    source.addEventListener('error', () => {
      setStatus(
        source.readyState === EventSource.CLOSED ? { kind: 'lost' } : { kind: 'reconnecting' },
      );
    });
    return () => {
      source.close();
      setStatus({ kind: 'connecting' });
    };
  }, [eventsUrl, attempt]);

  return target === undefined ? undefined : status;
}

function parseProblem(data: string): ProblemDetail {
  try {
    const problem: unknown = JSON.parse(data);
    if (typeof problem === 'object' && problem !== null) {
      return problem as ProblemDetail;
    }
  } catch {
    // Reported below.
  }
  return { title: 'Read failed', detail: 'Reading the destination failed.' };
}
