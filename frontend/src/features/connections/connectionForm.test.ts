import { describe, expect, it } from 'vitest';
import {
  credentialsAfterBrokerChange,
  credentialsChoices,
  existingConnectionValues,
  newConnectionValues,
  toCreateRequest,
  toUpdateRequest,
  validateConnection,
  type ConnectionFormValues,
} from './connectionForm';
import { aConnection } from '../../test/connectionFixtures';

function values(overrides: Partial<ConnectionFormValues>): ConnectionFormValues {
  return { ...newConnectionValues(), ...overrides };
}

describe('credentialsChoices', () => {
  it('offers only the credentials the broker supports', () => {
    expect(credentialsChoices('PULSAR', false)).toEqual(['NONE', 'TOKEN']);
    expect(credentialsChoices('KAFKA', false)).toEqual(['NONE', 'USERNAME_PASSWORD']);
    expect(credentialsChoices('RABBITMQ', false)).toEqual(['NONE', 'USERNAME_PASSWORD']);
  });

  it('offers to keep the current credentials when editing', () => {
    expect(credentialsChoices('KAFKA', true)).toEqual(['KEEP', 'NONE', 'USERNAME_PASSWORD']);
  });
});

describe('credentialsAfterBrokerChange', () => {
  it('keeps credentials the new broker supports', () => {
    expect(credentialsAfterBrokerChange('USERNAME_PASSWORD', 'RABBITMQ')).toBe('USERNAME_PASSWORD');
  });

  it('falls back to none when the new broker does not support them', () => {
    expect(credentialsAfterBrokerChange('USERNAME_PASSWORD', 'PULSAR')).toBe('NONE');
  });
});

describe('validateConnection', () => {
  it('requires a name and an address', () => {
    const blank = values({ name: '  ', address: '' });

    expect(validateConnection.name?.(blank.name, blank)).toBe('Enter a name');
    expect(validateConnection.address?.(blank.address, blank)).toBe('Enter the broker address');
  });

  it('limits the name length', () => {
    const long = values({ name: 'n'.repeat(101) });

    expect(validateConnection.name?.(long.name, long)).toBe('Use at most 100 characters');
  });

  it('requires the secrets of the chosen credentials only', () => {
    const withToken = values({ credentialsType: 'TOKEN' });
    const withoutCredentials = values({ credentialsType: 'NONE' });

    expect(validateConnection.token?.('', withToken)).toBe('Enter the token');
    expect(validateConnection.token?.('', withoutCredentials)).toBeNull();
    expect(validateConnection.password?.('', withoutCredentials)).toBeNull();
  });
});

describe('toCreateRequest', () => {
  it('trims the name and address and sends the credentials', () => {
    const request = toCreateRequest(
      values({
        name: ' Local ',
        address: ' localhost:9092 ',
        credentialsType: 'USERNAME_PASSWORD',
        username: ' alice ',
        password: ' secret ',
      }),
    );

    expect(request).toEqual({
      name: 'Local',
      brokerType: 'KAFKA',
      address: 'localhost:9092',
      credentials: { type: 'USERNAME_PASSWORD', username: 'alice', password: ' secret ' },
    });
  });
});

describe('toUpdateRequest', () => {
  it('sends only the name when only the name changed', () => {
    const connection = aConnection({ credentialsType: 'USERNAME_PASSWORD' });

    const request = toUpdateRequest(connection, {
      ...existingConnectionValues(connection),
      name: 'Renamed',
    });

    expect(request).toEqual({ name: 'Renamed' });
  });

  it('sends new credentials when they are replaced', () => {
    const connection = aConnection();

    const request = toUpdateRequest(connection, {
      ...existingConnectionValues(connection),
      credentialsType: 'USERNAME_PASSWORD',
      username: 'alice',
      password: 'secret',
    });

    expect(request).toEqual({
      credentials: { type: 'USERNAME_PASSWORD', username: 'alice', password: 'secret' },
    });
  });
});
