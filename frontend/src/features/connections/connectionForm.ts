import type {
  BrokerType,
  ConnectionResponse,
  CreateConnectionRequest,
  CredentialsRequest,
  CredentialsType,
  SaslMechanism,
  TlsSettings,
  UpdateConnectionRequest,
} from '../../api/connection';
import { BROKER_SETTINGS } from './brokers';

/** `KEEP` leaves an existing connection's credentials as they are, as the API never returns them. */
export type CredentialsChoice = CredentialsType | 'KEEP';

export type ConnectionFormValues = {
  name: string;
  brokerType: BrokerType;
  address: string;
  credentialsType: CredentialsChoice;
  token: string;
  username: string;
  password: string;
  /** Kafka only, for a username and password. */
  saslMechanism: SaslMechanism;
  /** Kafka only: Pulsar and RabbitMQ switch TLS on with their address scheme. */
  tlsEnabled: boolean;
  trustedCertificates: string;
};

const NAME_MAX_LENGTH = 100;
const ADDRESS_MAX_LENGTH = 2048;
const CERTIFICATES_MAX_LENGTH = 65536;
const PEM_CERTIFICATE = '-----BEGIN CERTIFICATE-----';

const BLANK_SECRETS = { token: '', username: '', password: '' };

export function newConnectionValues(): ConnectionFormValues {
  return {
    name: '',
    brokerType: 'KAFKA',
    address: '',
    credentialsType: 'NONE',
    ...BLANK_SECRETS,
    saslMechanism: 'PLAIN',
    tlsEnabled: false,
    trustedCertificates: '',
  };
}

export function existingConnectionValues(connection: ConnectionResponse): ConnectionFormValues {
  return {
    name: connection.name,
    brokerType: connection.brokerType,
    address: connection.address,
    credentialsType: 'KEEP',
    ...BLANK_SECRETS,
    saslMechanism: connection.saslMechanism ?? 'PLAIN',
    tlsEnabled: connection.tls.enabled ?? false,
    trustedCertificates: connection.tls.trustedCertificates ?? '',
  };
}

/** Whether the connection will use TLS: Kafka's flag, or the other brokers' address scheme. */
export function tlsInUse(values: ConnectionFormValues): boolean {
  const scheme = BROKER_SETTINGS[values.brokerType].tlsScheme;
  return scheme === undefined
    ? values.tlsEnabled
    : values.address.trim().toLowerCase().startsWith(scheme);
}

export function credentialsChoices(brokerType: BrokerType, editing: boolean): CredentialsChoice[] {
  const supported = BROKER_SETTINGS[brokerType].credentials;
  return editing ? ['KEEP', ...supported] : supported;
}

/** Keeps the chosen credentials when the new broker supports them, otherwise falls back to none. */
export function credentialsAfterBrokerChange(
  credentialsType: CredentialsChoice,
  brokerType: BrokerType,
): CredentialsChoice {
  return credentialsType === 'KEEP' ||
    BROKER_SETTINGS[brokerType].credentials.includes(credentialsType)
    ? credentialsType
    : 'NONE';
}

type Validator = (value: string, values: ConnectionFormValues) => string | null;

/** The form fields that hold text, which are the ones validated. */
type TextField = {
  [K in keyof ConnectionFormValues]: ConnectionFormValues[K] extends string ? K : never;
}[keyof ConnectionFormValues];

export const validateConnection: Partial<Record<TextField, Validator>> = {
  name: (value) => requiredWithin(value, NAME_MAX_LENGTH, 'Enter a name'),
  address: (value) => requiredWithin(value, ADDRESS_MAX_LENGTH, 'Enter the broker address'),
  token: (value, values) =>
    values.credentialsType === 'TOKEN' && value === '' ? 'Enter the token' : null,
  username: (value, values) =>
    values.credentialsType === 'USERNAME_PASSWORD' && value.trim() === ''
      ? 'Enter the username'
      : null,
  password: (value, values) =>
    values.credentialsType === 'USERNAME_PASSWORD' && value === '' ? 'Enter the password' : null,
  trustedCertificates: (value, values) => {
    const trimmed = value.trim();
    if (!tlsInUse(values) || trimmed === '') {
      return null;
    }
    if (trimmed.includes('PRIVATE KEY-----')) {
      return 'Paste only CA certificates, never a private key';
    }
    if (!trimmed.includes(PEM_CERTIFICATE)) {
      return `Paste PEM certificates, each starting with ${PEM_CERTIFICATE}`;
    }
    return trimmed.length > CERTIFICATES_MAX_LENGTH
      ? `Use at most ${String(CERTIFICATES_MAX_LENGTH)} characters`
      : null;
  },
};

function requiredWithin(value: string, maxLength: number, missing: string): string | null {
  const trimmed = value.trim();
  if (trimmed === '') {
    return missing;
  }
  return trimmed.length > maxLength ? `Use at most ${String(maxLength)} characters` : null;
}

export function toCreateRequest(values: ConnectionFormValues): CreateConnectionRequest {
  return {
    name: values.name.trim(),
    brokerType: values.brokerType,
    address: values.address.trim(),
    credentials: toCredentials(values) ?? { type: 'NONE' },
    ...(tlsInUse(values) && { tls: toTls(values) }),
  };
}

/** Sends only what changed, so renaming a connection never touches its credentials. */
export function toUpdateRequest(
  connection: ConnectionResponse,
  values: ConnectionFormValues,
): UpdateConnectionRequest {
  const name = values.name.trim();
  const address = values.address.trim();
  const credentials = toCredentials(values);
  const tls = toTls(values);
  const tlsChanged =
    (tls.enabled ?? false) !== (connection.tls.enabled ?? false) ||
    tls.trustedCertificates !== connection.tls.trustedCertificates;
  return {
    ...(name !== connection.name && { name }),
    ...(address !== connection.address && { address }),
    ...(credentials !== undefined && { credentials }),
    ...(tlsChanged && { tls }),
  };
}

/** Trusted certificates are kept only while TLS is in use, as the broker would ignore them. */
function toTls(values: ConnectionFormValues): TlsSettings {
  const usesFlag = BROKER_SETTINGS[values.brokerType].tlsScheme === undefined;
  const certificates = values.trustedCertificates.trim();
  return {
    enabled: usesFlag && values.tlsEnabled,
    ...(tlsInUse(values) && certificates !== '' && { trustedCertificates: certificates }),
  };
}

function toCredentials(values: ConnectionFormValues): CredentialsRequest | undefined {
  switch (values.credentialsType) {
    case 'KEEP':
      return undefined;
    case 'NONE':
      return { type: 'NONE' };
    case 'TOKEN':
      return { type: 'TOKEN', token: values.token };
    case 'USERNAME_PASSWORD':
      return {
        type: 'USERNAME_PASSWORD',
        username: values.username.trim(),
        password: values.password,
        ...(values.brokerType === 'KAFKA' && { saslMechanism: values.saslMechanism }),
      };
  }
}
