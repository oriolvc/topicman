import type { BrokerType, CredentialsType } from '../../api/connection';

type BrokerDescription = {
  label: string;
  addressPlaceholder: string;
  /** Mirrors the connection domain's rule; the API rejects any other kind. */
  credentials: CredentialsType[];
};

export const BROKERS: Record<BrokerType, BrokerDescription> = {
  KAFKA: {
    label: 'Kafka',
    addressPlaceholder: 'localhost:9092',
    credentials: ['NONE', 'USERNAME_PASSWORD'],
  },
  PULSAR: {
    label: 'Pulsar',
    addressPlaceholder: 'pulsar://localhost:6650',
    credentials: ['NONE', 'TOKEN'],
  },
  RABBITMQ: {
    label: 'RabbitMQ',
    addressPlaceholder: 'amqp://localhost:5672',
    credentials: ['NONE', 'USERNAME_PASSWORD'],
  },
};

export const BROKER_TYPES: BrokerType[] = ['KAFKA', 'PULSAR', 'RABBITMQ'];

export const CREDENTIALS_LABELS: Record<CredentialsType, string> = {
  NONE: 'None',
  TOKEN: 'Token',
  USERNAME_PASSWORD: 'Username and password',
};
