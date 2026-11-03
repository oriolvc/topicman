import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import type { ConnectionResponse } from '../../api/connection';
import { brokersHandler } from '../../test/brokerFixtures';
import { aConnection } from '../../test/connectionFixtures';
import { server } from '../../test/server';
import { renderAppAt } from '../../test/renderWithProviders';

const KAFKA = aConnection({ id: 1, name: 'Local Kafka', brokerType: 'KAFKA' });
const RABBIT = aConnection({
  id: 2,
  name: 'Shared RabbitMQ',
  brokerType: 'RABBITMQ',
  address: 'amqp://rabbit:5672',
});

function serveConnections(connections: ConnectionResponse[]) {
  server.use(http.get('/api/v1/connections', () => HttpResponse.json(connections)));
}

/** Records the produce requests and answers each copy with a Kafka-style receipt. */
function acceptMessages() {
  const requests: unknown[] = [];
  server.use(
    http.post('/api/v1/messages', async ({ request }) => {
      const body = (await request.json()) as { copies?: number };
      requests.push(body);
      const receipts = Array.from(
        { length: body.copies ?? 1 },
        (_, index) => `orders-0@${String(40 + index)}`,
      );
      return HttpResponse.json({ receipts });
    }),
  );
  return requests;
}

async function chooseConnection(user: ReturnType<typeof userEvent.setup>, label: string) {
  await user.click(screen.getByRole('combobox', { name: /Connection/ }));
  await user.click(await screen.findByRole('option', { name: label }));
}

/** JSON contains braces, which `user.type` treats as key descriptors. */
function enterPayload(payload: string) {
  fireEvent.change(screen.getByRole('textbox', { name: /Payload/ }), {
    target: { value: payload },
  });
}

describe('ProducePage', () => {
  beforeEach(() => {
    server.use(brokersHandler);
  });

  it('sends a JSON message with key, headers and copies, and shows the receipts', async () => {
    const user = userEvent.setup();
    serveConnections([KAFKA]);
    const requests = acceptMessages();
    renderAppAt('/produce');

    await chooseConnection(user, 'Local Kafka (Kafka)');
    await user.type(screen.getByRole('textbox', { name: /Topic/ }), 'orders');
    await user.type(screen.getByRole('textbox', { name: /Key/ }), 'customer-7');
    await user.click(screen.getByRole('button', { name: 'Add header' }));
    await user.type(screen.getByRole('textbox', { name: 'Header 1 name' }), 'trace-id');
    await user.type(screen.getByRole('textbox', { name: 'Header 1 value' }), 'abc');
    enterPayload('{"id": 1}');
    const copies = screen.getByRole('textbox', { name: 'Copies' });
    await user.clear(copies);
    await user.type(copies, '2');
    await user.click(screen.getByRole('button', { name: 'Send' }));

    const result = await screen.findByRole('region', { name: 'Result' });
    expect(await within(result).findByText('2 copies sent')).toBeDefined();
    expect(within(result).getByText('orders-0@40')).toBeDefined();
    expect(within(result).getByText('orders-0@41')).toBeDefined();
    expect(requests).toEqual([
      {
        connectionId: 1,
        destination: { type: 'TOPIC', name: 'orders' },
        key: 'customer-7',
        headers: { 'trace-id': 'abc' },
        payload: { format: 'JSON', content: '{"id": 1}' },
        copies: 2,
      },
    ]);
  });

  it('asks RabbitMQ for an exchange and routing key instead of a topic and key', async () => {
    const user = userEvent.setup();
    serveConnections([KAFKA, RABBIT]);
    const requests = acceptMessages();
    renderAppAt('/produce');

    await chooseConnection(user, 'Shared RabbitMQ (RabbitMQ)');
    await user.type(screen.getByRole('textbox', { name: /Exchange/ }), 'events');
    await user.type(screen.getByRole('textbox', { name: /Routing key/ }), 'orders.created');
    await user.click(screen.getByRole('radio', { name: 'Text' }));
    await user.type(screen.getByRole('textbox', { name: /Payload/ }), 'hello');
    await user.click(screen.getByRole('button', { name: 'Send' }));

    expect(await screen.findByText('Message sent')).toBeDefined();
    expect(screen.queryByRole('textbox', { name: /Topic/ })).toBeNull();
    expect(screen.queryByRole('textbox', { name: /^Key/ })).toBeNull();
    expect(requests).toEqual([
      {
        connectionId: 2,
        destination: { type: 'EXCHANGE', name: 'events', routingKey: 'orders.created' },
        payload: { format: 'TEXT', content: 'hello' },
        copies: 1,
      },
    ]);
  });

  it('does not send a payload that is not valid JSON', async () => {
    const user = userEvent.setup();
    serveConnections([KAFKA]);
    const requests = acceptMessages();
    renderAppAt('/produce');

    await chooseConnection(user, 'Local Kafka (Kafka)');
    await user.type(screen.getByRole('textbox', { name: /Topic/ }), 'orders');
    enterPayload('{"id":');
    await user.click(screen.getByRole('button', { name: 'Send' }));

    expect(await screen.findByText('The payload is not valid JSON')).toBeDefined();
    expect(requests).toEqual([]);
  });

  it('says how many copies were sent before a failure', async () => {
    const user = userEvent.setup();
    serveConnections([KAFKA]);
    server.use(
      http.post('/api/v1/messages', () =>
        HttpResponse.json(
          {
            title: 'Produce failed',
            status: 503,
            detail: "Could not produce to topic 'orders': timed out",
            code: 'messaging.produce-failed',
            sent: 2,
          },
          { status: 503 },
        ),
      ),
    );
    renderAppAt('/produce?connection=1');

    await user.type(await screen.findByRole('textbox', { name: /Topic/ }), 'orders');
    enterPayload('{}');
    const copies = screen.getByRole('textbox', { name: 'Copies' });
    await user.clear(copies);
    await user.type(copies, '5');
    await user.click(screen.getByRole('button', { name: 'Send' }));

    const result = await screen.findByRole('region', { name: 'Result' });
    expect(await within(result).findByText('Produce failed')).toBeDefined();
    expect(
      within(result).getByText("Could not produce to topic 'orders': timed out"),
    ).toBeDefined();
    expect(within(result).getByText('2 of 5 copies were sent before the failure.')).toBeDefined();
  });

  it('keeps the chosen connection in the URL', async () => {
    const user = userEvent.setup();
    serveConnections([KAFKA, RABBIT]);
    const router = renderAppAt('/produce');

    await chooseConnection(user, 'Shared RabbitMQ (RabbitMQ)');

    expect(router.state.location.search).toBe('?connection=2');
  });

  it('starts with the connection from the URL', async () => {
    serveConnections([KAFKA, RABBIT]);

    renderAppAt('/produce?connection=2');

    await waitFor(() => {
      expect(screen.getByRole<HTMLInputElement>('combobox', { name: /Connection/ }).value).toBe(
        'Shared RabbitMQ (RabbitMQ)',
      );
    });
    expect(screen.getByRole('textbox', { name: /Exchange/ })).toBeDefined();
  });

  it('points to the connections screen when there are none', async () => {
    serveConnections([]);

    renderAppAt('/produce');

    expect(await screen.findByRole('link', { name: 'Create a connection' })).toBeDefined();
  });
});
