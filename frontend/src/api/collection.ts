import createClient from 'openapi-fetch';
import { clientOptions } from './clientOptions';
import type { components, paths } from './generated/collection';

type Schemas = components['schemas'];

export type CollectionSummaryResponse = Schemas['CollectionSummaryResponse'];
export type CollectionResponse = Schemas['CollectionResponse'];
export type TemplateRequest = Schemas['TemplateRequest'];
export type TemplateResponse = Schemas['TemplateResponse'];
export type ResolvedMessageResponse = Schemas['ResolvedMessageResponse'];
export type UnresolvedVariablesProblem = Schemas['UnresolvedVariablesProblem'];
export type EnvironmentRequest = Schemas['EnvironmentRequest'];
export type EnvironmentResponse = Schemas['EnvironmentResponse'];
export type Variable = Schemas['Variable'];

/** Typed client for the collections and environments API (`collection.yaml`). */
export const collectionApi = createClient<paths>(clientOptions);
