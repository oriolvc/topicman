import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import type { CollectionResponse } from '../../api/collection';
import { brokersHandler } from '../../test/brokerFixtures';
import { aCollection, aTemplate, summaryOf } from '../../test/collectionFixtures';
import { aConnection } from '../../test/connectionFixtures';
import { renderAppAt } from '../../test/renderWithProviders';
import { server } from '../../test/server';

const KAFKA = aConnection({ id: 1, name: 'Local Kafka', brokerType: 'KAFKA' });
const RABBIT = aConnection({ id: 2, name: 'Shared RabbitMQ', brokerType: 'RABBITMQ' });

/** Serves collections and records the template and collection writes. */
function serveCollections(collections: CollectionResponse[]) {
  const requests: { method: string; path: string; body: unknown }[] = [];
  server.use(
    http.get('/api/v1/collections', () => HttpResponse.json(collections.map(summaryOf))),
    http.get('/api/v1/collections/:collectionId', ({ params }) =>
      HttpResponse.json(collections.find(({ id }) => String(id) === params['collectionId'])),
    ),
    http.post('/api/v1/collections', async ({ request }) => {
      const body = (await request.json()) as { name: string };
      requests.push({ method: 'POST', path: '/collections', body });
      return HttpResponse.json(aCollection({ id: 7, name: body.name, templates: [] }), {
        status: 201,
      });
    }),
    http.post('/api/v1/collections/:collectionId/templates', async ({ request, params }) => {
      const body = (await request.json()) as { name: string };
      requests.push({
        method: 'POST',
        path: `/collections/${String(params['collectionId'])}/templates`,
        body,
      });
      return HttpResponse.json(aTemplate({ id: 30, name: body.name }), { status: 201 });
    }),
    http.put(
      '/api/v1/collections/:collectionId/templates/:templateId',
      async ({ request, params }) => {
        const body = (await request.json()) as { name: string };
        const path = `/collections/${String(params['collectionId'])}/templates/${String(params['templateId'])}`;
        requests.push({ method: 'PUT', path, body });
        return HttpResponse.json(aTemplate({ id: Number(params['templateId']), name: body.name }));
      },
    ),
  );
  return requests;
}

/** Braces are key descriptors for `user.type`, and templates are full of them. */
function setText(name: RegExp, value: string) {
  fireEvent.change(screen.getByRole('textbox', { name }), { target: { value } });
}

async function chooseConnection(user: ReturnType<typeof userEvent.setup>, label: string) {
  await user.click(screen.getByRole('combobox', { name: /Connection/ }));
  await user.click(await screen.findByRole('option', { name: label }));
}

describe('ProducePage with templates', () => {
  beforeEach(() => {
    server.use(
      brokersHandler,
      http.get('/api/v1/connections', () => HttpResponse.json([KAFKA, RABBIT])),
    );
  });

  it('saves the message as a template, placeholders and all', async () => {
    const user = userEvent.setup();
    const requests = serveCollections([aCollection({ id: 1, name: 'Orders' })]);
    const router = renderAppAt('/produce');

    await chooseConnection(user, 'Local Kafka (Kafka)');
    setText(/Topic/, 'orders-{{env}}');
    setText(/^Key/, '{{orderId}}');
    // Not valid JSON until resolved, which a template may be.
    setText(/Payload/, '{"id": {{orderId}}}');
    await user.click(screen.getByRole('button', { name: 'Save as template' }));
    const dialog = screen.getByRole('dialog', { name: 'Save as template' });
    await user.click(within(dialog).getByRole('combobox', { name: 'Collection' }));
    await user.click(await screen.findByRole('option', { name: 'Orders' }));
    await user.type(
      within(dialog).getByRole('textbox', { name: /Template name/ }),
      'Order created',
    );
    await user.click(within(dialog).getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Template Order created saved to Orders')).toBeDefined();
    expect(requests).toEqual([
      {
        method: 'POST',
        path: '/collections/1/templates',
        body: {
          name: 'Order created',
          brokerType: 'KAFKA',
          destination: { type: 'TOPIC', name: 'orders-{{env}}' },
          key: '{{orderId}}',
          headers: {},
          payload: { format: 'JSON', content: '{"id": {{orderId}}}' },
        },
      },
    ]);
    await waitFor(() => {
      expect(router.state.location.search).toBe('?connection=1&collection=1&template=30');
    });
  });

  it('saves a template into a new collection', async () => {
    const user = userEvent.setup();
    const requests = serveCollections([]);
    renderAppAt('/produce');

    await chooseConnection(user, 'Shared RabbitMQ (RabbitMQ)');
    setText(/Exchange/, 'events');
    await user.click(screen.getByRole('radio', { name: 'Text' }));
    setText(/Payload/, 'hello {{name}}');
    await user.click(screen.getByRole('button', { name: 'Save as template' }));
    const dialog = screen.getByRole('dialog', { name: 'Save as template' });
    await user.type(
      within(dialog).getByRole('textbox', { name: /New collection name/ }),
      'Greetings',
    );
    await user.type(within(dialog).getByRole('textbox', { name: /Template name/ }), 'Hello');
    await user.click(within(dialog).getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Template Hello saved to Greetings')).toBeDefined();
    expect(requests.map(({ method, path }) => `${method} ${path}`)).toEqual([
      'POST /collections',
      'POST /collections/7/templates',
    ]);
    expect(requests[1]?.body).toEqual({
      name: 'Hello',
      brokerType: 'RABBITMQ',
      destination: { type: 'EXCHANGE', name: 'events', routingKey: '' },
      headers: {},
      payload: { format: 'TEXT', content: 'hello {{name}}' },
    });
  });

  it('shows a taken template name next to the field', async () => {
    const user = userEvent.setup();
    serveCollections([aCollection({ id: 1, name: 'Orders' })]);
    server.use(
      http.post('/api/v1/collections/:collectionId/templates', () =>
        HttpResponse.json(
          { title: 'Template name taken', status: 409, code: 'collection.template-name-taken' },
          { status: 409 },
        ),
      ),
    );
    renderAppAt('/produce');

    await chooseConnection(user, 'Local Kafka (Kafka)');
    setText(/Topic/, 'orders');
    await user.click(screen.getByRole('button', { name: 'Save as template' }));
    const dialog = screen.getByRole('dialog', { name: 'Save as template' });
    await user.click(within(dialog).getByRole('combobox', { name: 'Collection' }));
    await user.click(await screen.findByRole('option', { name: 'Orders' }));
    await user.type(
      within(dialog).getByRole('textbox', { name: /Template name/ }),
      'Order created',
    );
    await user.click(within(dialog).getByRole('button', { name: 'Save' }));

    expect(
      await within(dialog).findByText('The collection already has a template with this name'),
    ).toBeDefined();
  });

  it('asks for a connection before saving, to know the broker', async () => {
    const user = userEvent.setup();
    serveCollections([]);
    renderAppAt('/produce');

    await user.click(await screen.findByRole('button', { name: 'Save as template' }));

    expect(await screen.findByText('Choose a connection')).toBeDefined();
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('opens a template from its collection and saves it back', async () => {
    const user = userEvent.setup();
    const requests = serveCollections([
      aCollection({ id: 1, name: 'Orders', templates: [aTemplate({ id: 10 })] }),
    ]);
    renderAppAt('/produce?collection=1&template=10');

    expect(await screen.findByText(/Editing the template/)).toBeDefined();
    await waitFor(() => {
      expect(screen.getByRole<HTMLInputElement>('textbox', { name: /Topic/ }).value).toBe(
        'orders-{{env}}',
      );
    });
    expect(screen.getByRole<HTMLInputElement>('combobox', { name: /Connection/ }).value).toBe(
      'Local Kafka (Kafka)',
    );
    expect(screen.getByRole<HTMLInputElement>('textbox', { name: 'Header 1 value' }).value).toBe(
      '{{$uuid}}',
    );
    setText(/Topic/, 'orders-v2-{{env}}');
    await user.click(screen.getByRole('button', { name: 'Save template' }));

    expect(await screen.findByText('Template Order created saved')).toBeDefined();
    expect(requests).toEqual([
      {
        method: 'PUT',
        path: '/collections/1/templates/10',
        body: {
          name: 'Order created',
          brokerType: 'KAFKA',
          destination: { type: 'TOPIC', name: 'orders-v2-{{env}}' },
          key: '{{orderId}}',
          headers: { 'trace-id': '{{$uuid}}' },
          payload: { format: 'JSON', content: '{"id": "{{orderId}}"}' },
        },
      },
    ]);
  });

  it('says that placeholders are sent as written from this screen', async () => {
    const user = userEvent.setup();
    serveCollections([]);
    renderAppAt('/produce');

    await chooseConnection(user, 'Local Kafka (Kafka)');
    setText(/Topic/, 'orders-{{env}}');

    expect(await screen.findByText(/sent as written/)).toBeDefined();
  });
});
