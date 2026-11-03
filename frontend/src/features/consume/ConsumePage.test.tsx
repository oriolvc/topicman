import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse, sse, type ServerSentEventMessage } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import type { ConsumedMessageEvent, ConsumeSessionResponse } from '../../api/messaging';
import { brokersHandler } from '../../test/brokerFixtures';
import { aConnection } from '../../test/connectionFixtures';
import { renderAppAt } from '../../test/renderWithProviders';
import { server } from '../../test/server';

const KAFKA = aConnection({ id: 'kafka-1', name: 'Local Kafka', brokerType: 'KAFKA' });

let sessions = 0;

/** A new id per test, as a test's unmount closes its session after the next test has started. */
function aSession(): ConsumeSessionResponse {
  const id = `session-${String(++sessions)}`;
  return {
    id,
    connectionId: 'kafka-1',
    destination: { type: 'TOPIC', name: 'orders' },
    startPosition: 'LATEST',
    openedAt: '2026-10-19T10:00:00Z',
    eventsUrl: `/api/v1/consume-sessions/${id}/events`,
  };
}

type SessionEvents = { message: string; failed: string; closed: string; replaced: string };

/** One connection to the event stream, which the test sends events through. */
type StreamClient = { send(payload: ServerSentEventMessage<SessionEvents>): void };

/** Serves a Kafka connection and one session, recording what the screen asks for. */
function serveASession() {
  const session = aSession();
  const opened: unknown[] = [];
  const closed: string[] = [];
  const clients: StreamClient[] = [];
  server.use(
    http.get('/api/v1/connections', () => HttpResponse.json([KAFKA])),
    http.post('/api/v1/consume-sessions', async ({ request }) => {
      opened.push(await request.json());
      return HttpResponse.json(session, { status: 201 });
    }),
    http.delete('/api/v1/consume-sessions/:sessionId', ({ params }) => {
      if (params['sessionId'] === session.id) {
        closed.push(session.id);
      }
      return new HttpResponse(null, { status: 204 });
    }),
    sse<SessionEvents>('/api/v1/consume-sessions/:sessionId/events', ({ client }) => {
      // MSW's mocked fetch only resolves, and so opens the stream, once something is sent.
      client.send({ retry: 1000 });
      clients.push(client);
    }),
  );
  return { session, opened, closed, clients };
}

function send(client: StreamClient | undefined, message: ConsumedMessageEvent) {
  client?.send({ event: 'message', id: message.position, data: JSON.stringify(message) });
}

function aMessage(overrides: Partial<ConsumedMessageEvent> = {}): ConsumedMessageEvent {
  return {
    headers: {},
    payload: { format: 'JSON', content: '{"id":1}' },
    timestamp: '2026-10-19T10:00:01Z',
    position: 'orders-0@0',
    ...overrides,
  };
}

async function startConsuming(user: ReturnType<typeof userEvent.setup>) {
  renderAppAt('/consume');
  await user.click(screen.getByRole('combobox', { name: /Connection/ }));
  await user.click(await screen.findByRole('option', { name: 'Local Kafka (Kafka)' }));
  await user.type(screen.getByRole('textbox', { name: /Topic/ }), 'orders');
  await user.click(screen.getByRole('button', { name: 'Start' }));
  await screen.findByText('Streaming');
}

function payloadOf(message: HTMLElement): string | null {
  return within(message).getByLabelText('Payload').textContent;
}

describe('ConsumePage', () => {
  beforeEach(() => {
    server.use(brokersHandler);
  });

  it('explains that a broker which cannot read from the beginning only reads new messages', async () => {
    const user = userEvent.setup();
    const rabbit = aConnection({ id: 'rabbit-1', name: 'Shared RabbitMQ', brokerType: 'RABBITMQ' });
    server.use(http.get('/api/v1/connections', () => HttpResponse.json([rabbit])));
    renderAppAt('/consume');

    await user.click(screen.getByRole('combobox', { name: /Connection/ }));
    await user.click(await screen.findByRole('option', { name: 'Shared RabbitMQ (RabbitMQ)' }));

    expect(
      await screen.findByText('RabbitMQ only reads messages published from now'),
    ).toBeDefined();
    expect(screen.getByRole('radio', { name: 'Earliest' }).hasAttribute('disabled')).toBe(true);
    expect(screen.getByRole('textbox', { name: /Exchange/ })).toBeDefined();
  });

  it('streams messages newest first, with their key, headers and formatted payload', async () => {
    const user = userEvent.setup();
    const { opened, clients } = serveASession();
    renderAppAt('/consume');

    await user.click(screen.getByRole('combobox', { name: /Connection/ }));
    await user.click(await screen.findByRole('option', { name: 'Local Kafka (Kafka)' }));
    await user.type(screen.getByRole('textbox', { name: /Topic/ }), 'orders');
    await user.click(screen.getByRole('radio', { name: 'Earliest' }));
    await user.click(screen.getByRole('button', { name: 'Start' }));
    await screen.findByText('Streaming');
    send(
      clients[0],
      aMessage({ position: 'orders-0@0', payload: { format: 'TEXT', content: 'hi' } }),
    );
    send(
      clients[0],
      aMessage({
        position: 'orders-0@1',
        key: 'customer-7',
        headers: { 'trace-id': 'abc' },
        payload: { format: 'JSON', content: '{"id":2}' },
      }),
    );

    await waitFor(() => {
      expect(screen.getAllByRole('article')).toHaveLength(2);
    });
    const [newest, oldest] = screen.getAllByRole('article');
    if (newest === undefined || oldest === undefined) {
      throw new Error('Expected two messages');
    }
    expect(newest.getAttribute('aria-label')).toBe('Message at orders-0@1');
    expect(within(newest).getByText('key: customer-7')).toBeDefined();
    expect(within(newest).getByText('trace-id: abc')).toBeDefined();
    expect(payloadOf(newest)).toBe('{\n  "id": 2\n}');
    expect(payloadOf(oldest)).toBe('hi');
    expect(screen.getByText('2 messages')).toBeDefined();
    expect(opened).toEqual([
      {
        connectionId: 'kafka-1',
        destination: { type: 'TOPIC', name: 'orders' },
        startPosition: 'EARLIEST',
      },
    ]);
  });

  it('shows payloads as received when raw, and clears the list', async () => {
    const user = userEvent.setup();
    const { clients } = serveASession();
    await startConsuming(user);
    send(clients[0], aMessage({ payload: { format: 'JSON', content: '{"id":1}' } }));
    const message = await screen.findByRole('article');

    await user.click(screen.getByRole('radio', { name: 'Raw' }));
    expect(payloadOf(message)).toBe('{"id":1}');

    await user.click(screen.getByRole('button', { name: 'Clear' }));
    expect(screen.queryByRole('article')).toBeNull();
    expect(screen.getByText('0 messages')).toBeDefined();
  });

  it('pauses by disconnecting, and resumes the same session', async () => {
    const user = userEvent.setup();
    const { opened, closed, clients } = serveASession();
    await startConsuming(user);

    await user.click(screen.getByRole('button', { name: 'Pause' }));
    expect(await screen.findByText('Paused')).toBeDefined();
    await user.click(screen.getByRole('button', { name: 'Resume' }));
    await screen.findByText('Streaming');
    send(clients[1], aMessage({ position: 'orders-0@5' }));

    expect(await screen.findByRole('article', { name: 'Message at orders-0@5' })).toBeDefined();
    expect(clients).toHaveLength(2);
    expect(opened).toHaveLength(1);
    expect(closed).toEqual([]);
  });

  it('closes the session when stopped', async () => {
    const user = userEvent.setup();
    const { session, closed } = serveASession();
    await startConsuming(user);

    await user.click(screen.getByRole('button', { name: 'Stop' }));

    await waitFor(() => {
      expect(closed).toEqual([session.id]);
    });
    expect(screen.getByRole('button', { name: 'Start' })).toBeDefined();
    expect(screen.getByRole('textbox', { name: /Topic/ })).toHaveProperty('disabled', false);
  });

  it('closes the session when leaving the screen', async () => {
    const user = userEvent.setup();
    const { session, closed } = serveASession();
    await startConsuming(user);

    await user.click(screen.getByRole('link', { name: 'Produce' }));

    await waitFor(() => {
      expect(closed).toEqual([session.id]);
    });
  });

  it('stops when another window takes the session over, and can take it back', async () => {
    const user = userEvent.setup();
    const { clients } = serveASession();
    await startConsuming(user);

    clients[0]?.send({ event: 'replaced', data: '' });
    expect(await screen.findByText('Streaming in another window')).toBeDefined();
    await user.click(screen.getByRole('button', { name: 'Resume here' }));

    expect(await screen.findByText('Streaming')).toBeDefined();
    expect(clients).toHaveLength(2);
  });

  it('shows why reading failed', async () => {
    const user = userEvent.setup();
    const { clients } = serveASession();
    await startConsuming(user);

    clients[0]?.send({
      event: 'failed',
      data: JSON.stringify({
        title: 'Read failed',
        status: 503,
        detail: 'Connection to the broker was lost',
        code: 'messaging.read-failed',
      }),
    });

    expect(await screen.findByText('Connection to the broker was lost')).toBeDefined();
    expect(screen.getByRole('button', { name: 'Start' })).toBeDefined();
  });

  it('reports a lost stream when the session no longer exists', async () => {
    const user = userEvent.setup();
    serveASession();
    server.use(
      http.get('/api/v1/consume-sessions/:sessionId/events', () =>
        HttpResponse.json(
          { title: 'Consume session not found', status: 404, code: 'messaging.session-not-found' },
          { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    renderAppAt('/consume?connection=kafka-1');

    await user.type(await screen.findByRole('textbox', { name: /Topic/ }), 'orders');
    await user.click(screen.getByRole('button', { name: 'Start' }));

    expect(await screen.findByText('The stream was lost')).toBeDefined();
  });

  it('shows why a session could not be opened', async () => {
    const user = userEvent.setup();
    serveASession();
    server.use(
      http.post('/api/v1/consume-sessions', () =>
        HttpResponse.json(
          {
            title: 'Read failed',
            status: 503,
            detail: 'Topic orders does not exist',
            code: 'messaging.read-failed',
          },
          { status: 503, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    renderAppAt('/consume?connection=kafka-1');

    await user.type(await screen.findByRole('textbox', { name: /Topic/ }), 'orders');
    await user.click(screen.getByRole('button', { name: 'Start' }));

    expect(await screen.findByText('Topic orders does not exist')).toBeDefined();
  });

  it('asks for a destination before starting', async () => {
    const user = userEvent.setup();
    const { opened } = serveASession();
    renderAppAt('/consume?connection=kafka-1');

    await user.click(await screen.findByRole('button', { name: 'Start' }));

    expect(await screen.findByText('Enter a topic')).toBeDefined();
    expect(opened).toEqual([]);
  });
});
