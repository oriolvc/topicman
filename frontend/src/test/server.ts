import { setupServer } from 'msw/node';

/** The mocked backend for every test; each test adds the handlers it needs with `server.use()`. */
export const server = setupServer();
