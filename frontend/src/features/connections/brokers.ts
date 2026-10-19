import type { BrokerType, CredentialsType } from '../../api/connection';

type BrokerSettings = {
  addressPlaceholder: string;
  /** Mirrors the connection domain's rule; the API rejects any other kind. */
  credentials: CredentialsType[];
};

export const BROKER_SETTINGS: Record<BrokerType, BrokerSettings> = {
  KAFKA: {
    addressPlaceholder: 'localhost:9092',
    credentials: ['NONE', 'USERNAME_PASSWORD'],
  },
  PULSAR: {
    addressPlaceholder: 'pulsar://localhost:6650',
    credentials: ['NONE', 'TOKEN'],
  },
  RABBITMQ: {
    addressPlaceholder: 'amqp://localhost:5672',
    credentials: ['NONE', 'USERNAME_PASSWORD'],
  },
};

export const CREDENTIALS_LABELS: Record<CredentialsType, string> = {
  NONE: 'None',
  TOKEN: 'Token',
  USERNAME_PASSWORD: 'Username and password',
};
