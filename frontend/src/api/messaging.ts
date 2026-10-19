import createClient from 'openapi-fetch';
import { clientOptions } from './clientOptions';
import type { components, paths } from './generated/messaging';

type Schemas = components['schemas'];

export type Destination = Schemas['Destination'];
export type Headers = Schemas['Headers'];
export type Payload = Schemas['Payload'];
export type StartPosition = Schemas['StartPosition'];
export type ProduceMessageRequest = Schemas['ProduceMessageRequest'];
export type ProduceMessageResponse = Schemas['ProduceMessageResponse'];
export type ProduceFailedProblem = Schemas['ProduceFailedProblem'];
export type OpenConsumeSessionRequest = Schemas['OpenConsumeSessionRequest'];
export type ConsumeSessionResponse = Schemas['ConsumeSessionResponse'];
/** The `data` of a `message` event on a consume session's event stream. */
export type ConsumedMessageEvent = Schemas['ConsumedMessageEvent'];

/**
 * Typed client for the messaging API (`messaging.yaml`). The event stream of a consume session is
 * read with an `EventSource` instead, at the session's `eventsUrl`.
 */
export const messagingApi = createClient<paths>(clientOptions);
