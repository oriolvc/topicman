import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { anEnvironment } from '../test/environmentFixtures';
import { server } from '../test/server';
import { renderAppAt } from '../test/renderWithProviders';

describe('AppLayout', () => {
  afterEach(() => {
    window.localStorage.clear();
  });

  beforeEach(() => {
    server.use(http.get('/api/v1/connections', () => HttpResponse.json([])));
  });

  it('shows the application name', () => {
    renderAppAt('/connections');

    expect(screen.getByRole('heading', { name: 'TopicMan' })).toBeDefined();
  });

  it('opens the connections screen from the root URL', async () => {
    const router = renderAppAt('/');

    expect(await screen.findByRole('heading', { name: 'Connections' })).toBeDefined();
    expect(router.state.location.pathname).toBe('/connections');
  });

  it('shows a not found page for an unknown URL', () => {
    renderAppAt('/nowhere');

    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeDefined();
  });

  it('remembers the environment chosen in the header', async () => {
    const user = userEvent.setup();
    server.use(
      http.get('/api/v1/environments', () =>
        HttpResponse.json([
          anEnvironment({ id: 1, name: 'dev' }),
          anEnvironment({ id: 2, name: 'staging' }),
        ]),
      ),
    );
    renderAppAt('/connections');

    await user.click(screen.getByRole('combobox', { name: 'Environment' }));
    await user.click(await screen.findByRole('option', { name: 'staging' }));

    expect(window.localStorage.getItem('topicman.activeEnvironmentId')).toBe('"2"');
    expect(screen.getByRole<HTMLInputElement>('combobox', { name: 'Environment' }).value).toBe(
      'staging',
    );
  });

  it('switches to dark mode', async () => {
    const user = userEvent.setup();
    renderAppAt('/connections');

    await user.click(screen.getByRole('switch', { name: 'Dark mode' }));

    expect(screen.getByRole<HTMLInputElement>('switch', { name: 'Dark mode' }).checked).toBe(true);
  });
});
