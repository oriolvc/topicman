import { BROKERS } from '../../test/brokerFixtures';
import { describe, expect, it } from 'vitest';
import { initialConsumeValues, toOpenRequest, validateConsume } from './consumeForm';

describe('validateConsume', () => {
  it('asks for a connection first', () => {
    expect(validateConsume(initialConsumeValues(null), undefined)).toEqual({
      connectionId: 'Choose a connection',
    });
  });

  it('asks for the kind of destination the broker uses', () => {
    const values = initialConsumeValues('1');

    expect(validateConsume(values, BROKERS.KAFKA)).toEqual({ destinationName: 'Enter a topic' });
    expect(validateConsume(values, BROKERS.RABBITMQ)).toEqual({
      destinationName: 'Enter an exchange',
    });
  });

  it('accepts a destination', () => {
    expect(
      validateConsume({ ...initialConsumeValues('1'), destinationName: 'orders' }, BROKERS.KAFKA),
    ).toEqual({});
  });
});

describe('toOpenRequest', () => {
  it('reads a topic from the chosen position', () => {
    const values = {
      ...initialConsumeValues('1'),
      destinationName: ' orders ',
      startPosition: 'EARLIEST' as const,
    };

    expect(toOpenRequest(values, 1, BROKERS.KAFKA)).toEqual({
      connectionId: 1,
      destination: { type: 'TOPIC', name: 'orders' },
      startPosition: 'EARLIEST',
    });
  });

  it('binds to an exchange with its routing key, reading only new messages', () => {
    const values = {
      ...initialConsumeValues('1'),
      destinationName: 'events',
      routingKey: 'orders.*',
      startPosition: 'EARLIEST' as const,
    };

    expect(toOpenRequest(values, 1, BROKERS.RABBITMQ)).toEqual({
      connectionId: 1,
      destination: { type: 'EXCHANGE', name: 'events', routingKey: 'orders.*' },
      startPosition: 'LATEST',
    });
  });
});
