import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

/**
 * The mocked backend for every test; each test adds the handlers it needs with `server.use()`.
 * The header's environment picker asks for environments on every screen, so by default there are
 * none.
 */
export const server = setupServer(http.get('/api/v1/environments', () => HttpResponse.json([])));
