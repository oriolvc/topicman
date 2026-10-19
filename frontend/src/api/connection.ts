import createClient from 'openapi-fetch';
import { clientOptions } from './clientOptions';
import type { components, paths } from './generated/connection';

type Schemas = components['schemas'];

export type BrokerType = Schemas['BrokerType'];
export type CredentialsType = Schemas['CredentialsType'];
export type ConnectionResponse = Schemas['ConnectionResponse'];
export type CreateConnectionRequest = Schemas['CreateConnectionRequest'];
export type UpdateConnectionRequest = Schemas['UpdateConnectionRequest'];
export type CredentialsRequest = Schemas['CredentialsRequest'];

/** Typed client for the connections API (`connection.yaml`). */
export const connectionApi = createClient<paths>(clientOptions);
