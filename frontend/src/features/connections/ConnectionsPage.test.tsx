import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import type { ConnectionResponse } from '../../api/connection';
import { server } from '../../test/server';
import { renderAppAt } from '../../test/renderWithProviders';
import { aConnection } from '../../test/connectionFixtures';

const CONNECTIONS = '/api/v1/connections';
const CONNECTION = '/api/v1/connections/:connectionId';

/** A tiny in-memory backend, so a test sees the list change after each request. */
function serveConnections(initial: ConnectionResponse[]) {
  let connections = [...initial];
  const requests: { method: string; body: unknown }[] = [];
  server.use(
    http.get(CONNECTIONS, () => HttpResponse.json(connections)),
    http.post(CONNECTIONS, async ({ request }) => {
      const body = (await request.json()) as { name: string; brokerType: 'KAFKA' };
      requests.push({ method: 'POST', body });
      const created = aConnection({ id: 'new-id', name: body.name, brokerType: body.brokerType });
      connections = [...connections, created];
      return HttpResponse.json(created, { status: 201 });
    }),
    http.patch(CONNECTION, async ({ request, params }) => {
      const body = (await request.json()) as Partial<ConnectionResponse>;
      requests.push({ method: 'PATCH', body });
      connections = connections.map((connection) =>
        connection.id === params['connectionId'] ? { ...connection, ...body } : connection,
      );
      return HttpResponse.json(connections.find(({ id }) => id === params['connectionId']));
    }),
    http.delete(CONNECTION, ({ params }) => {
      requests.push({ method: 'DELETE', body: params['connectionId'] });
      connections = connections.filter(({ id }) => id !== params['connectionId']);
      return new HttpResponse(null, { status: 204 });
    }),
  );
  return requests;
}

describe('ConnectionsPage', () => {
  it('lists the connections', async () => {
    serveConnections([
      aConnection({ id: '1', name: 'Local Kafka' }),
      aConnection({
        id: '2',
        name: 'Staging Pulsar',
        brokerType: 'PULSAR',
        address: 'pulsar://staging:6650',
        credentialsType: 'TOKEN',
      }),
    ]);

    renderAppAt('/connections');

    const row = await screen.findByRole('row', { name: /Staging Pulsar/ });
    expect(within(row).getByText('Pulsar')).toBeDefined();
    expect(within(row).getByText('pulsar://staging:6650')).toBeDefined();
    expect(within(row).getByText('Token')).toBeDefined();
    expect(screen.getByRole('row', { name: /Local Kafka/ })).toBeDefined();
  });

  it('says when there are no connections', async () => {
    serveConnections([]);

    renderAppAt('/connections');

    expect(await screen.findByText(/No connections yet/)).toBeDefined();
  });

  it('shows why the connections could not be loaded', async () => {
    server.use(
      http.get(CONNECTIONS, () =>
        HttpResponse.json(
          { title: 'Bad Request', status: 400, detail: 'Something is off' },
          { status: 400 },
        ),
      ),
    );

    renderAppAt('/connections');

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByText('Something is off')).toBeDefined();
  });

  it('creates a connection', async () => {
    const user = userEvent.setup();
    const requests = serveConnections([]);
    renderAppAt('/connections');

    await user.click(await screen.findByRole('button', { name: 'New connection' }));
    const dialog = screen.getByRole('dialog', { name: 'New connection' });
    await user.type(within(dialog).getByLabelText(/Name/), 'Shared RabbitMQ');
    await user.click(within(dialog).getByRole('radio', { name: 'RabbitMQ' }));
    await user.type(within(dialog).getByLabelText(/Address/), 'amqp://rabbit:5672');
    await user.click(within(dialog).getByRole('radio', { name: 'Username and password' }));
    await user.type(within(dialog).getByRole('textbox', { name: /Username/ }), 'guest');
    await user.type(within(dialog).getByLabelText(/^Password/), 'guest-secret');
    await user.click(within(dialog).getByRole('button', { name: 'Create' }));

    expect(await screen.findByRole('row', { name: /Shared RabbitMQ/ })).toBeDefined();
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(requests).toEqual([
      {
        method: 'POST',
        body: {
          name: 'Shared RabbitMQ',
          brokerType: 'RABBITMQ',
          address: 'amqp://rabbit:5672',
          credentials: { type: 'USERNAME_PASSWORD', username: 'guest', password: 'guest-secret' },
        },
      },
    ]);
  });

  it('validates the form before sending it', async () => {
    const user = userEvent.setup();
    const requests = serveConnections([]);
    renderAppAt('/connections');

    await user.click(await screen.findByRole('button', { name: 'New connection' }));
    await user.click(screen.getByRole('button', { name: 'Create' }));

    expect(screen.getByText('Enter a name')).toBeDefined();
    expect(screen.getByText('Enter the broker address')).toBeDefined();
    expect(requests).toEqual([]);
  });

  it('shows a taken name next to the name field', async () => {
    const user = userEvent.setup();
    serveConnections([]);
    server.use(
      http.post(CONNECTIONS, () =>
        HttpResponse.json(
          { title: 'Conflict', status: 409, code: 'connection.name-taken' },
          { status: 409 },
        ),
      ),
    );
    renderAppAt('/connections');

    await user.click(await screen.findByRole('button', { name: 'New connection' }));
    await user.type(screen.getByLabelText(/Name/), 'Local Kafka');
    await user.type(screen.getByLabelText(/Address/), 'localhost:9092');
    await user.click(screen.getByRole('button', { name: 'Create' }));

    expect(await screen.findByText('Another connection already has this name')).toBeDefined();
    expect(screen.getByRole('dialog', { name: 'New connection' })).toBeDefined();
  });

  it('tests a connection and shows what answered', async () => {
    const user = userEvent.setup();
    serveConnections([aConnection({ id: 'kafka-1', name: 'Local Kafka' })]);
    const tested: unknown[] = [];
    server.use(
      http.post('/api/v1/connection-tests', async ({ request }) => {
        tested.push(await request.json());
        return HttpResponse.json({
          status: 'SUCCEEDED',
          detail: 'Kafka cluster abc, 1 broker',
          durationMs: 42,
        });
      }),
    );
    renderAppAt('/connections');

    await user.click(await screen.findByRole('button', { name: 'Test Local Kafka' }));

    expect(await screen.findByText('Local Kafka works')).toBeDefined();
    expect(screen.getByText('Kafka cluster abc, 1 broker (42 ms)')).toBeDefined();
    expect(tested).toEqual([{ connectionId: 'kafka-1' }]);
  });

  it('shows why a connection test failed', async () => {
    const user = userEvent.setup();
    serveConnections([aConnection({ name: 'Local Kafka' })]);
    server.use(
      http.post('/api/v1/connection-tests', () =>
        HttpResponse.json({
          status: 'FAILED',
          detail: 'Cannot reach the Kafka broker at localhost:9092: Connection refused',
          durationMs: 3,
        }),
      ),
    );
    renderAppAt('/connections');

    await user.click(await screen.findByRole('button', { name: 'Test Local Kafka' }));

    expect(await screen.findByText('Local Kafka failed the test')).toBeDefined();
    expect(
      screen.getByText('Cannot reach the Kafka broker at localhost:9092: Connection refused'),
    ).toBeDefined();
  });

  it('notifies other failures with the problem detail', async () => {
    const user = userEvent.setup();
    serveConnections([]);
    server.use(
      http.post(CONNECTIONS, () =>
        HttpResponse.json(
          {
            title: 'Invalid connection',
            status: 400,
            detail: 'RabbitMQ does not support token credentials',
            code: 'connection.invalid-connection',
          },
          { status: 400 },
        ),
      ),
    );
    renderAppAt('/connections');

    await user.click(await screen.findByRole('button', { name: 'New connection' }));
    await user.type(screen.getByLabelText(/Name/), 'Local Kafka');
    await user.type(screen.getByLabelText(/Address/), 'localhost:9092');
    await user.click(screen.getByRole('button', { name: 'Create' }));

    expect(await screen.findByText('RabbitMQ does not support token credentials')).toBeDefined();
    expect(screen.getByText('Invalid connection')).toBeDefined();
  });

  it('renames a connection without touching its credentials', async () => {
    const user = userEvent.setup();
    const requests = serveConnections([
      aConnection({ id: '1', name: 'Local Kafka', credentialsType: 'USERNAME_PASSWORD' }),
    ]);
    renderAppAt('/connections');

    await user.click(await screen.findByRole('button', { name: 'Edit Local Kafka' }));
    const dialog = screen.getByRole('dialog', { name: 'Edit connection' });
    expect(within(dialog).getByText(/Current: Username and password/)).toBeDefined();
    const name = within(dialog).getByLabelText(/Name/);
    await user.clear(name);
    await user.type(name, 'Dev Kafka');
    await user.click(within(dialog).getByRole('button', { name: 'Save' }));

    expect(await screen.findByRole('row', { name: /Dev Kafka/ })).toBeDefined();
    expect(requests).toEqual([{ method: 'PATCH', body: { name: 'Dev Kafka' } }]);
  });

  it('does not let the broker of a connection change', async () => {
    const user = userEvent.setup();
    serveConnections([aConnection({ id: '1', name: 'Local Kafka' })]);
    renderAppAt('/connections');

    await user.click(await screen.findByRole('button', { name: 'Edit Local Kafka' }));

    expect(screen.getByRole<HTMLInputElement>('radio', { name: 'Pulsar' }).disabled).toBe(true);
  });

  it('deletes a connection after confirmation', async () => {
    const user = userEvent.setup();
    const requests = serveConnections([aConnection({ id: '1', name: 'Local Kafka' })]);
    renderAppAt('/connections');

    await user.click(await screen.findByRole('button', { name: 'Delete Local Kafka' }));
    const dialog = screen.getByRole('dialog', { name: 'Delete connection' });
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));

    expect(await screen.findByText(/No connections yet/)).toBeDefined();
    await waitFor(() => {
      expect(requests).toEqual([{ method: 'DELETE', body: '1' }]);
    });
  });

  it('keeps the connection when deletion is cancelled', async () => {
    const user = userEvent.setup();
    const requests = serveConnections([aConnection({ id: '1', name: 'Local Kafka' })]);
    renderAppAt('/connections');

    await user.click(await screen.findByRole('button', { name: 'Delete Local Kafka' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(screen.getByRole('row', { name: /Local Kafka/ })).toBeDefined();
    expect(requests).toEqual([]);
  });
});
