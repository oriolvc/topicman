import type {
  BrokerType,
  ConnectionResponse,
  CreateConnectionRequest,
  CredentialsRequest,
  CredentialsType,
  UpdateConnectionRequest,
} from '../../api/connection';
import { BROKERS } from './brokers';

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
};

const NAME_MAX_LENGTH = 100;
const ADDRESS_MAX_LENGTH = 2048;

const BLANK_SECRETS = { token: '', username: '', password: '' };

export function newConnectionValues(): ConnectionFormValues {
  return { name: '', brokerType: 'KAFKA', address: '', credentialsType: 'NONE', ...BLANK_SECRETS };
}

export function existingConnectionValues(connection: ConnectionResponse): ConnectionFormValues {
  return {
    name: connection.name,
    brokerType: connection.brokerType,
    address: connection.address,
    credentialsType: 'KEEP',
    ...BLANK_SECRETS,
  };
}

export function credentialsChoices(brokerType: BrokerType, editing: boolean): CredentialsChoice[] {
  const supported = BROKERS[brokerType].credentials;
  return editing ? ['KEEP', ...supported] : supported;
}

/** Keeps the chosen credentials when the new broker supports them, otherwise falls back to none. */
export function credentialsAfterBrokerChange(
  credentialsType: CredentialsChoice,
  brokerType: BrokerType,
): CredentialsChoice {
  return credentialsType === 'KEEP' || BROKERS[brokerType].credentials.includes(credentialsType)
    ? credentialsType
    : 'NONE';
}

type Validator = (value: string, values: ConnectionFormValues) => string | null;

export const validateConnection: Partial<Record<keyof ConnectionFormValues, Validator>> = {
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
  return {
    ...(name !== connection.name && { name }),
    ...(address !== connection.address && { address }),
    ...(credentials !== undefined && { credentials }),
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
      };
  }
}
