import type { BrokerType, CredentialsType, SaslMechanism } from '../../api/connection';

type BrokerSettings = {
  addressPlaceholder: string;
  /** Mirrors the connection domain's rule; the API rejects any other kind. */
  credentials: CredentialsType[];
  /**
   * The address scheme that switches TLS on (ADR 0008), or `undefined` for Kafka, whose address has
   * no scheme and which uses the TLS flag instead.
   */
  tlsScheme: string | undefined;
};

export const BROKER_SETTINGS: Record<BrokerType, BrokerSettings> = {
  KAFKA: {
    addressPlaceholder: 'localhost:9092',
    credentials: ['NONE', 'USERNAME_PASSWORD'],
    tlsScheme: undefined,
  },
  PULSAR: {
    addressPlaceholder: 'pulsar://localhost:6650',
    credentials: ['NONE', 'TOKEN'],
    tlsScheme: 'pulsar+ssl://',
  },
  RABBITMQ: {
    addressPlaceholder: 'amqp://localhost:5672',
    credentials: ['NONE', 'USERNAME_PASSWORD'],
    tlsScheme: 'amqps://',
  },
};

export const SASL_MECHANISMS: SaslMechanism[] = ['PLAIN', 'SCRAM_SHA_256', 'SCRAM_SHA_512'];

export const SASL_MECHANISM_LABELS: Record<SaslMechanism, string> = {
  PLAIN: 'PLAIN',
  SCRAM_SHA_256: 'SCRAM-SHA-256',
  SCRAM_SHA_512: 'SCRAM-SHA-512',
};

export const CREDENTIALS_LABELS: Record<CredentialsType, string> = {
  NONE: 'None',
  TOKEN: 'Token',
  USERNAME_PASSWORD: 'Username and password',
};
