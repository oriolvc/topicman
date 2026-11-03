import { http, HttpResponse } from 'msw';
import type { BrokerType } from '../api/connection';
import type { Broker } from '../api/messaging';

/** The brokers as `GET /api/v1/brokers` describes them. */
export const BROKERS: Record<BrokerType, Broker> = {
  PULSAR: {
    type: 'PULSAR',
    destinationType: 'TOPIC',
    canReadFromBeginning: true,
    supportsMessageKeys: true,
  },
  KAFKA: {
    type: 'KAFKA',
    destinationType: 'TOPIC',
    canReadFromBeginning: true,
    supportsMessageKeys: true,
  },
  RABBITMQ: {
    type: 'RABBITMQ',
    destinationType: 'EXCHANGE',
    canReadFromBeginning: false,
    supportsMessageKeys: false,
  },
};

/** Answers `GET /api/v1/brokers` with every broker. */
export const brokersHandler = http.get('/api/v1/brokers', () =>
  HttpResponse.json([BROKERS.PULSAR, BROKERS.KAFKA, BROKERS.RABBITMQ]),
);
