import type { ClientOptions } from 'openapi-fetch';

/**
 * Same-origin calls (in development the Vite dev server proxies them). `fetch` is looked up on
 * every call rather than captured once, so tools that wrap it later, such as MSW in tests, apply.
 */
export const clientOptions: ClientOptions = {
  baseUrl: window.location.origin,
  fetch: (request) => globalThis.fetch(request),
};
