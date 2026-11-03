import type { Broker, Destination } from '../api/messaging';

/** The destination fields of a form that produces to or consumes from a destination. */
export type DestinationValues = {
  /** The topic, or the exchange for a broker that uses exchanges. */
  destinationName: string;
  routingKey: string;
};

export function destinationError(values: DestinationValues, broker: Broker): string | undefined {
  if (values.destinationName.trim() !== '') {
    return undefined;
  }
  return broker.destinationType === 'TOPIC' ? 'Enter a topic' : 'Enter an exchange';
}

export function toDestination(values: DestinationValues, broker: Broker): Destination {
  const name = values.destinationName.trim();
  return broker.destinationType === 'TOPIC'
    ? { type: 'TOPIC', name }
    : { type: 'EXCHANGE', name, routingKey: values.routingKey };
}
