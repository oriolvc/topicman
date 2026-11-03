import { BROKERS } from '../../test/brokerFixtures';
import { describe, expect, it } from 'vitest';
import {
  initialProduceValues,
  toProduceRequest,
  validateProduce,
  type ProduceFormValues,
} from './produceForm';

function values(overrides: Partial<ProduceFormValues>): ProduceFormValues {
  return {
    ...initialProduceValues('connection-1'),
    destinationName: 'orders',
    payload: '{"id": 1}',
    ...overrides,
  };
}

describe('validateProduce', () => {
  it('accepts a complete message', () => {
    expect(validateProduce(values({}), BROKERS.KAFKA)).toEqual({});
  });

  it('requires a saved connection', () => {
    expect(validateProduce(values({}), undefined)).toEqual({ connectionId: 'Choose a connection' });
  });

  it('names the destination the broker expects', () => {
    const blank = values({ destinationName: ' ' });

    expect(validateProduce(blank, BROKERS.PULSAR)['destinationName']).toBe('Enter a topic');
    expect(validateProduce(blank, BROKERS.RABBITMQ)['destinationName']).toBe('Enter an exchange');
  });

  it('rejects a payload that is not valid JSON', () => {
    expect(validateProduce(values({ payload: '{"id":' }), BROKERS.KAFKA)).toEqual({
      payload: 'The payload is not valid JSON',
    });
  });

  it('accepts any text payload, even empty', () => {
    expect(validateProduce(values({ payloadFormat: 'TEXT', payload: '' }), BROKERS.KAFKA)).toEqual(
      {},
    );
  });

  it('rejects blank and repeated header names', () => {
    const errors = validateProduce(
      values({
        headers: [
          { key: 'a', name: 'trace', value: '1' },
          { key: 'b', name: ' ', value: '2' },
          { key: 'c', name: 'trace ', value: '3' },
        ],
      }),
      BROKERS.KAFKA,
    );

    expect(errors).toEqual({
      'headers.1.name': 'Enter a header name',
      'headers.2.name': 'This header is already set',
    });
  });

  it('limits the number of copies', () => {
    expect(validateProduce(values({ copies: 101 }), BROKERS.KAFKA)['copies']).toBe(
      'Send between 1 and 100 copies',
    );
    expect(validateProduce(values({ copies: '' }), BROKERS.KAFKA)['copies']).toBeDefined();
  });
});

describe('toProduceRequest', () => {
  it('sends a topic message with key, headers and copies', () => {
    const request = toProduceRequest(
      values({
        destinationName: ' orders ',
        messageKey: 'customer-7',
        headers: [{ key: 'a', name: ' trace-id ', value: 'abc' }],
        copies: 3,
      }),
      'connection-1',
      BROKERS.KAFKA,
    );

    expect(request).toEqual({
      connectionId: 'connection-1',
      destination: { type: 'TOPIC', name: 'orders' },
      key: 'customer-7',
      headers: { 'trace-id': 'abc' },
      payload: { format: 'JSON', content: '{"id": 1}' },
      copies: 3,
    });
  });

  it('omits an empty key and no headers', () => {
    const request = toProduceRequest(values({}), 'connection-1', BROKERS.PULSAR);

    expect(request).not.toHaveProperty('key');
    expect(request).not.toHaveProperty('headers');
  });

  it('sends an exchange and routing key to RabbitMQ, without a key', () => {
    const request = toProduceRequest(
      values({ destinationName: 'events', routingKey: 'orders.created', messageKey: 'ignored' }),
      'connection-1',
      BROKERS.RABBITMQ,
    );

    expect(request.destination).toEqual({
      type: 'EXCHANGE',
      name: 'events',
      routingKey: 'orders.created',
    });
    expect(request).not.toHaveProperty('key');
  });
});
