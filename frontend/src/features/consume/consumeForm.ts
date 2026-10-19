import type { BrokerType } from '../../api/connection';
import type { OpenConsumeSessionRequest, StartPosition } from '../../api/messaging';
import { destinationError, toDestination, type DestinationValues } from '../../shared/destinations';

export type ConsumeFormValues = DestinationValues & {
  connectionId: string | null;
  startPosition: StartPosition;
};

export function initialConsumeValues(connectionId: string | null): ConsumeFormValues {
  return { connectionId, destinationName: '', routingKey: '', startPosition: 'LATEST' };
}

/** RabbitMQ only sees messages published after its temporary queue is bound (ADR 0003). */
export function supportsEarliest(brokerType: BrokerType): boolean {
  return brokerType !== 'RABBITMQ';
}

/** `brokerType` is the selected connection's broker, or `undefined` when none is selected. */
export function validateConsume(
  values: ConsumeFormValues,
  brokerType: BrokerType | undefined,
): Record<string, string> {
  if (brokerType === undefined) {
    return { connectionId: 'Choose a connection' };
  }
  const destination = destinationError(values, brokerType);
  return destination === undefined ? {} : { destinationName: destination };
}

export function toOpenRequest(
  values: ConsumeFormValues,
  connectionId: string,
  brokerType: BrokerType,
): OpenConsumeSessionRequest {
  return {
    connectionId,
    destination: toDestination(values, brokerType),
    startPosition: supportsEarliest(brokerType) ? values.startPosition : 'LATEST',
  };
}
