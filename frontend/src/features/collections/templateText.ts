import type { TemplateResponse } from '../../api/collection';

/** A destination as one line: the topic, or the exchange and routing key. */
export function describeDestination(destination: TemplateResponse['destination']): string {
  if (destination.type === 'TOPIC') {
    return destination.name;
  }
  return destination.routingKey === ''
    ? destination.name
    : `${destination.name} / ${destination.routingKey}`;
}
