import { describe, expect, it } from 'vitest';
import {
  credentialsAfterBrokerChange,
  credentialsChoices,
  existingConnectionValues,
  newConnectionValues,
  toCreateRequest,
  tlsInUse,
  toUpdateRequest,
  validateConnection,
  type ConnectionFormValues,
} from './connectionForm';
import { aConnection } from '../../test/connectionFixtures';

const PEM = '-----BEGIN CERTIFICATE-----\nMIIB\n-----END CERTIFICATE-----';

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
      credentials: {
        type: 'USERNAME_PASSWORD',
        username: 'alice',
        password: ' secret ',
        saslMechanism: 'PLAIN',
      },
    });
  });

  it('sends the chosen SASL mechanism for Kafka only', () => {
    const kafka = toCreateRequest(
      values({
        credentialsType: 'USERNAME_PASSWORD',
        username: 'app',
        password: 'pw',
        saslMechanism: 'SCRAM_SHA_512',
      }),
    );
    const rabbit = toCreateRequest(
      values({
        brokerType: 'RABBITMQ',
        credentialsType: 'USERNAME_PASSWORD',
        username: 'app',
        password: 'pw',
        saslMechanism: 'SCRAM_SHA_512',
      }),
    );

    expect(kafka.credentials).toEqual({
      type: 'USERNAME_PASSWORD',
      username: 'app',
      password: 'pw',
      saslMechanism: 'SCRAM_SHA_512',
    });
    expect(rabbit.credentials).toEqual({
      type: 'USERNAME_PASSWORD',
      username: 'app',
      password: 'pw',
    });
  });

  it('sends no TLS settings when TLS is not in use', () => {
    const request = toCreateRequest(values({ address: 'kafka:9092', trustedCertificates: PEM }));

    expect(request.tls).toBeUndefined();
  });

  it('switches TLS on for Kafka with the flag, with the trusted certificates', () => {
    const request = toCreateRequest(
      values({ address: 'kafka:9093', tlsEnabled: true, trustedCertificates: `\n${PEM}\n` }),
    );

    expect(request.tls).toEqual({ enabled: true, trustedCertificates: PEM });
  });

  it('sends trusted certificates for a TLS address scheme, without the flag', () => {
    const request = toCreateRequest(
      values({ brokerType: 'RABBITMQ', address: 'amqps://rabbit:5671', trustedCertificates: PEM }),
    );

    expect(request.tls).toEqual({ enabled: false, trustedCertificates: PEM });
  });
});

describe('tlsInUse', () => {
  it('follows the flag for Kafka and the address scheme for the others', () => {
    expect(tlsInUse(values({ tlsEnabled: true }))).toBe(true);
    expect(tlsInUse(values({ address: 'kafka:9093' }))).toBe(false);
    expect(tlsInUse(values({ brokerType: 'PULSAR', address: ' Pulsar+SSL://p:6651' }))).toBe(true);
    expect(tlsInUse(values({ brokerType: 'PULSAR', address: 'pulsar://p:6650' }))).toBe(false);
    expect(tlsInUse(values({ brokerType: 'RABBITMQ', address: 'amqps://r:5671' }))).toBe(true);
    expect(tlsInUse(values({ brokerType: 'RABBITMQ', tlsEnabled: true }))).toBe(false);
  });
});

describe('validateConnection trusted certificates', () => {
  it('requires PEM certificates when some are given and TLS is in use', () => {
    const withTls = values({ tlsEnabled: true });
    const withoutTls = values({ tlsEnabled: false });

    expect(validateConnection.trustedCertificates?.('not a certificate', withTls)).toBe(
      'Paste PEM certificates, each starting with -----BEGIN CERTIFICATE-----',
    );
    expect(validateConnection.trustedCertificates?.('', withTls)).toBeNull();
    expect(validateConnection.trustedCertificates?.(PEM, withTls)).toBeNull();
    expect(validateConnection.trustedCertificates?.('not a certificate', withoutTls)).toBeNull();
  });

  it('refuses a private key, which would be stored and shown in plain text', () => {
    const withTls = values({ tlsEnabled: true });
    const withKey = `${PEM}\n-----BEGIN PRIVATE KEY-----\nMIIE\n-----END PRIVATE KEY-----`;

    expect(validateConnection.trustedCertificates?.(withKey, withTls)).toBe(
      'Paste only CA certificates, never a private key',
    );
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
      credentials: {
        type: 'USERNAME_PASSWORD',
        username: 'alice',
        password: 'secret',
        saslMechanism: 'PLAIN',
      },
    });
  });

  it('starts from the saved TLS settings and SASL mechanism', () => {
    const connection = aConnection({
      credentialsType: 'USERNAME_PASSWORD',
      saslMechanism: 'SCRAM_SHA_256',
      tls: { enabled: true, trustedCertificates: PEM },
    });

    expect(existingConnectionValues(connection)).toMatchObject({
      saslMechanism: 'SCRAM_SHA_256',
      tlsEnabled: true,
      trustedCertificates: PEM,
    });
  });

  it('sends the TLS settings only when they changed', () => {
    const connection = aConnection({ tls: { enabled: true, trustedCertificates: PEM } });

    const unchanged = toUpdateRequest(connection, existingConnectionValues(connection));
    const changed = toUpdateRequest(connection, {
      ...existingConnectionValues(connection),
      trustedCertificates: '',
    });

    expect(unchanged).toEqual({});
    expect(changed).toEqual({ tls: { enabled: true } });
  });
});
