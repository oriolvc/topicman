import type { Broker, OpenConsumeSessionRequest, StartPosition } from '../../api/messaging';
import { destinationError, toDestination, type DestinationValues } from '../../shared/destinations';

export type ConsumeFormValues = DestinationValues & {
  connectionId: string | null;
  startPosition: StartPosition;
};

export function initialConsumeValues(connectionId: string | null): ConsumeFormValues {
  return { connectionId, destinationName: '', routingKey: '', startPosition: 'LATEST' };
}

/** `broker` is the selected connection's broker, or `undefined` when none is selected. */
export function validateConsume(
  values: ConsumeFormValues,
  broker: Broker | undefined,
): Record<string, string> {
  if (broker === undefined) {
    return { connectionId: 'Choose a connection' };
  }
  const destination = destinationError(values, broker);
  return destination === undefined ? {} : { destinationName: destination };
}

export function toOpenRequest(
  values: ConsumeFormValues,
  connectionId: number,
  broker: Broker,
): OpenConsumeSessionRequest {
  return {
    connectionId,
    destination: toDestination(values, broker),
    // A broker that cannot read from the beginning treats EARLIEST as LATEST (ADR 0003).
    startPosition: broker.canReadFromBeginning ? values.startPosition : 'LATEST',
  };
}
