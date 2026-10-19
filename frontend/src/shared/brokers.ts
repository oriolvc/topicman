import type { BrokerType } from '../api/connection';
import type { Destination } from '../api/messaging';

export const BROKER_TYPES: BrokerType[] = ['KAFKA', 'PULSAR', 'RABBITMQ'];

export const BROKER_LABELS: Record<BrokerType, string> = {
  KAFKA: 'Kafka',
  PULSAR: 'Pulsar',
  RABBITMQ: 'RabbitMQ',
};

/**
 * The one kind of destination each broker supports, as the messaging API documents. Expected to
 * come from the API's broker capabilities once they exist (PLAN.md, Phase 3).
 */
export const DESTINATION_TYPES: Record<BrokerType, Destination['type']> = {
  KAFKA: 'TOPIC',
  PULSAR: 'TOPIC',
  RABBITMQ: 'EXCHANGE',
};
