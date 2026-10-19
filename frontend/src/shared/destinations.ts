import type { BrokerType } from '../api/connection';
import type { Destination } from '../api/messaging';
import { DESTINATION_TYPES } from './brokers';

/** The destination fields of a form that produces to or consumes from a destination. */
export type DestinationValues = {
  /** The topic, or the exchange for a broker that uses exchanges. */
  destinationName: string;
  routingKey: string;
};

export function destinationError(
  values: DestinationValues,
  brokerType: BrokerType,
): string | undefined {
  if (values.destinationName.trim() !== '') {
    return undefined;
  }
  return DESTINATION_TYPES[brokerType] === 'TOPIC' ? 'Enter a topic' : 'Enter an exchange';
}

export function toDestination(values: DestinationValues, brokerType: BrokerType): Destination {
  const name = values.destinationName.trim();
  return DESTINATION_TYPES[brokerType] === 'TOPIC'
    ? { type: 'TOPIC', name }
    : { type: 'EXCHANGE', name, routingKey: values.routingKey };
}
