import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import type { EnvironmentResponse } from '../../api/collection';
import { anEnvironment } from '../../test/environmentFixtures';
import { renderAppAt } from '../../test/renderWithProviders';
import { server } from '../../test/server';

const ENVIRONMENTS = '/api/v1/environments';
const ENVIRONMENT = '/api/v1/environments/:environmentId';

/** A tiny in-memory backend, so a test sees the list change after each request. */
function serveEnvironments(initial: EnvironmentResponse[]) {
  let environments = [...initial];
  const requests: { method: string; body: unknown }[] = [];
  server.use(
    http.get(ENVIRONMENTS, () => HttpResponse.json(environments)),
    http.post(ENVIRONMENTS, async ({ request }) => {
      const body = (await request.json()) as Pick<EnvironmentResponse, 'name' | 'variables'>;
      requests.push({ method: 'POST', body });
      const created = anEnvironment({ id: 99, ...body });
      environments = [...environments, created];
      return HttpResponse.json(created, { status: 201 });
    }),
    http.put(ENVIRONMENT, async ({ request, params }) => {
      const body = (await request.json()) as Pick<EnvironmentResponse, 'name' | 'variables'>;
      requests.push({ method: 'PUT', body });
      environments = environments.map((environment) =>
        String(environment.id) === params['environmentId']
          ? { ...environment, ...body }
          : environment,
      );
      return HttpResponse.json(
        environments.find(({ id }) => String(id) === params['environmentId']),
      );
    }),
    http.delete(ENVIRONMENT, ({ params }) => {
      requests.push({ method: 'DELETE', body: params['environmentId'] });
      environments = environments.filter(({ id }) => String(id) !== params['environmentId']);
      return new HttpResponse(null, { status: 204 });
    }),
  );
  return requests;
}

describe('EnvironmentsPage', () => {
  it('lists environments with their variable count', async () => {
    serveEnvironments([
      anEnvironment({ id: 1, name: 'dev', variables: [] }),
      anEnvironment({ id: 2, name: 'staging' }),
    ]);
    renderAppAt('/environments');

    const table = await screen.findByRole('table');
    expect(within(table).getByRole('row', { name: /dev.*No variables/ })).toBeDefined();
    expect(within(table).getByRole('row', { name: /staging.*1 variable/ })).toBeDefined();
  });

  it('says when there are no environments', async () => {
    serveEnvironments([]);
    renderAppAt('/environments');

    expect(await screen.findByText(/No environments yet/)).toBeDefined();
  });

  it('creates an environment with its variables in order', async () => {
    const user = userEvent.setup();
    const requests = serveEnvironments([]);
    renderAppAt('/environments');

    await user.click(await screen.findByRole('button', { name: 'New environment' }));
    const dialog = screen.getByRole('dialog', { name: 'New environment' });
    await user.type(within(dialog).getByRole('textbox', { name: /Name/ }), 'staging');
    await user.click(within(dialog).getByRole('button', { name: 'Add variable' }));
    await user.type(within(dialog).getByRole('textbox', { name: 'Variable 1 name' }), 'env');
    await user.type(within(dialog).getByRole('textbox', { name: 'Variable 1 value' }), 'stg');
    await user.click(within(dialog).getByRole('button', { name: 'Add variable' }));
    await user.type(within(dialog).getByRole('textbox', { name: 'Variable 2 name' }), 'region');
    await user.click(within(dialog).getByRole('button', { name: 'Create' }));

    expect(await screen.findByText('Environment staging created')).toBeDefined();
    expect(requests).toEqual([
      {
        method: 'POST',
        body: {
          name: 'staging',
          variables: [
            { name: 'env', value: 'stg' },
            { name: 'region', value: '' },
          ],
        },
      },
    ]);
  });

  it('shows a taken name next to the name field', async () => {
    const user = userEvent.setup();
    serveEnvironments([]);
    server.use(
      http.post(ENVIRONMENTS, () =>
        HttpResponse.json(
          {
            title: 'Environment name taken',
            status: 409,
            code: 'collection.environment-name-taken',
          },
          { status: 409 },
        ),
      ),
    );
    renderAppAt('/environments');

    await user.click(await screen.findByRole('button', { name: 'New environment' }));
    await user.type(screen.getByRole('textbox', { name: /Name/ }), 'staging');
    await user.click(screen.getByRole('button', { name: 'Create' }));

    expect(await screen.findByText('Another environment already has this name')).toBeDefined();
  });

  it('edits an environment', async () => {
    const user = userEvent.setup();
    const requests = serveEnvironments([anEnvironment({ id: 1, name: 'staging' })]);
    renderAppAt('/environments');

    await user.click(await screen.findByRole('button', { name: 'Edit staging' }));
    const dialog = screen.getByRole('dialog', { name: 'Edit environment' });
    const value = within(dialog).getByRole('textbox', { name: 'Variable 1 value' });
    await user.clear(value);
    await user.type(value, 'stage');
    await user.click(within(dialog).getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Environment staging saved')).toBeDefined();
    expect(requests).toEqual([
      { method: 'PUT', body: { name: 'staging', variables: [{ name: 'env', value: 'stage' }] } },
    ]);
  });

  it('deletes an environment after confirmation', async () => {
    const user = userEvent.setup();
    const requests = serveEnvironments([anEnvironment({ id: 1, name: 'staging' })]);
    renderAppAt('/environments');

    await user.click(await screen.findByRole('button', { name: 'Delete staging' }));
    await user.click(
      within(screen.getByRole('dialog', { name: 'Delete environment' })).getByRole('button', {
        name: 'Delete',
      }),
    );

    await waitFor(() => {
      expect(requests).toEqual([{ method: 'DELETE', body: '1' }]);
    });
    expect(await screen.findByText(/No environments yet/)).toBeDefined();
  });
});
