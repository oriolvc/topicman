import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { CollectionResponse } from '../../api/collection';
import { brokersHandler } from '../../test/brokerFixtures';
import { aCollection, aTemplate, summaryOf } from '../../test/collectionFixtures';
import { aConnection } from '../../test/connectionFixtures';
import { anEnvironment } from '../../test/environmentFixtures';
import { renderAppAt } from '../../test/renderWithProviders';
import { server } from '../../test/server';

const KAFKA = aConnection({ id: 1, name: 'Local Kafka', brokerType: 'KAFKA' });
const RABBIT = aConnection({ id: 2, name: 'Shared RabbitMQ', brokerType: 'RABBITMQ' });

/** A tiny in-memory backend for collections, so a test sees changes after each request. */
function serveCollections(initial: CollectionResponse[]) {
  let collections = [...initial];
  const requests: { method: string; path: string; body?: unknown }[] = [];
  const find = (id: unknown) => collections.find((collection) => String(collection.id) === id);
  server.use(
    http.get('/api/v1/collections', () => HttpResponse.json(collections.map(summaryOf))),
    http.get('/api/v1/collections/:collectionId', ({ params }) => {
      const collection = find(params['collectionId']);
      return collection === undefined
        ? HttpResponse.json({ status: 404, code: 'collection.not-found' }, { status: 404 })
        : HttpResponse.json(collection);
    }),
    http.post('/api/v1/collections', async ({ request }) => {
      const body = (await request.json()) as { name: string };
      requests.push({ method: 'POST', path: '/collections', body });
      const created = aCollection({ id: 7, name: body.name, templates: [] });
      collections = [...collections, created];
      return HttpResponse.json(created, { status: 201 });
    }),
    http.patch('/api/v1/collections/:collectionId', async ({ request, params }) => {
      const body = (await request.json()) as { name: string };
      requests.push({
        method: 'PATCH',
        path: `/collections/${String(params['collectionId'])}`,
        body,
      });
      collections = collections.map((collection) =>
        String(collection.id) === params['collectionId']
          ? { ...collection, name: body.name }
          : collection,
      );
      return HttpResponse.json(find(params['collectionId']));
    }),
    http.delete('/api/v1/collections/:collectionId', ({ params }) => {
      requests.push({ method: 'DELETE', path: `/collections/${String(params['collectionId'])}` });
      collections = collections.filter(({ id }) => String(id) !== params['collectionId']);
      return new HttpResponse(null, { status: 204 });
    }),
    http.delete('/api/v1/collections/:collectionId/templates/:templateId', ({ params }) => {
      const path = `/collections/${String(params['collectionId'])}/templates/${String(params['templateId'])}`;
      requests.push({ method: 'DELETE', path });
      collections = collections.map((collection) => ({
        ...collection,
        templates: collection.templates.filter(({ id }) => String(id) !== params['templateId']),
      }));
      return new HttpResponse(null, { status: 204 });
    }),
  );
  return requests;
}

describe('CollectionsPage', () => {
  beforeEach(() => {
    server.use(
      brokersHandler,
      http.get('/api/v1/connections', () => HttpResponse.json([KAFKA, RABBIT])),
    );
  });

  afterEach(() => {
    window.localStorage.clear();
  });

  it('lists collections and shows the templates of the chosen one', async () => {
    const user = userEvent.setup();
    serveCollections([
      aCollection({ id: 1, name: 'Orders' }),
      aCollection({
        id: 2,
        name: 'Shipping',
        templates: [
          aTemplate({
            id: 20,
            name: 'Shipped',
            brokerType: 'RABBITMQ',
            destination: { type: 'EXCHANGE', name: 'events', routingKey: 'shipped' },
          }),
        ],
      }),
    ]);
    const router = renderAppAt('/collections');

    await user.click(await screen.findByRole('link', { name: /Shipping/ }));

    expect(router.state.location.pathname).toBe('/collections/2');
    const row = await screen.findByRole('row', { name: /Shipped/ });
    expect(within(row).getByText('RabbitMQ')).toBeDefined();
    expect(within(row).getByText('events / shipped')).toBeDefined();
    expect(within(row).getByRole('link', { name: 'Edit Shipped' }).getAttribute('href')).toBe(
      '/produce?collection=2&template=20',
    );
  });

  it('creates a collection and opens it', async () => {
    const user = userEvent.setup();
    const requests = serveCollections([]);
    const router = renderAppAt('/collections');

    await user.click(await screen.findByRole('button', { name: 'New collection' }));
    const dialog = screen.getByRole('dialog', { name: 'New collection' });
    await user.type(within(dialog).getByRole('textbox', { name: /Name/ }), 'Billing');
    await user.click(within(dialog).getByRole('button', { name: 'Create' }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collections/7');
    });
    expect(requests).toEqual([{ method: 'POST', path: '/collections', body: { name: 'Billing' } }]);
    expect(await screen.findByText(/No templates yet/)).toBeDefined();
  });

  it('renames a collection', async () => {
    const user = userEvent.setup();
    const requests = serveCollections([aCollection({ id: 1, name: 'Orders' })]);
    renderAppAt('/collections/1');

    await user.click(await screen.findByRole('button', { name: 'Rename' }));
    const name = within(screen.getByRole('dialog', { name: 'Rename collection' })).getByRole(
      'textbox',
      {
        name: /Name/,
      },
    );
    await user.clear(name);
    await user.type(name, 'Billing');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByRole('heading', { name: 'Billing' })).toBeDefined();
    expect(requests).toEqual([
      { method: 'PATCH', path: '/collections/1', body: { name: 'Billing' } },
    ]);
  });

  it('deletes a collection after confirmation', async () => {
    const user = userEvent.setup();
    const requests = serveCollections([aCollection({ id: 1, name: 'Orders' })]);
    const router = renderAppAt('/collections/1');

    await user.click(await screen.findByRole('button', { name: 'Delete collection' }));
    await user.click(
      within(screen.getByRole('dialog', { name: 'Delete collection' })).getByRole('button', {
        name: 'Delete',
      }),
    );

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collections');
    });
    expect(requests).toEqual([{ method: 'DELETE', path: '/collections/1' }]);
  });

  it('deletes a template after confirmation', async () => {
    const user = userEvent.setup();
    const requests = serveCollections([aCollection({ id: 1 })]);
    renderAppAt('/collections/1');

    await user.click(await screen.findByRole('button', { name: 'Delete Order created' }));
    await user.click(
      within(screen.getByRole('dialog', { name: 'Delete template' })).getByRole('button', {
        name: 'Delete',
      }),
    );

    expect(await screen.findByText(/No templates yet/)).toBeDefined();
    expect(requests).toEqual([{ method: 'DELETE', path: '/collections/1/templates/10' }]);
  });

  it('previews and sends a template against the active environment', async () => {
    const user = userEvent.setup();
    serveCollections([aCollection({ id: 1 })]);
    window.localStorage.setItem('topicman.activeEnvironmentId', '"5"');
    const resolutions: unknown[] = [];
    const produced: unknown[] = [];
    const resolved = {
      brokerType: 'KAFKA',
      destination: { type: 'TOPIC', name: 'orders-stg' },
      key: '42',
      headers: { 'trace-id': 'abc' },
      payload: { format: 'JSON', content: '{"id": "42"}' },
    };
    server.use(
      http.get('/api/v1/environments', () =>
        HttpResponse.json([anEnvironment({ id: 5, name: 'staging' })]),
      ),
      http.post('/api/v1/collections/1/templates/10/resolution', async ({ request }) => {
        resolutions.push(await request.json());
        return HttpResponse.json(resolved);
      }),
      http.post('/api/v1/messages', async ({ request }) => {
        produced.push(await request.json());
        return HttpResponse.json({ receipts: ['orders-stg-0@1', 'orders-stg-0@2'] });
      }),
    );
    renderAppAt('/collections/1');

    await user.click(await screen.findByRole('button', { name: 'Send Order created' }));
    const dialog = screen.getByRole('dialog', { name: 'Send Order created' });
    await waitFor(() => {
      expect(
        within(dialog).getByRole<HTMLInputElement>('combobox', { name: 'Connection' }).value,
      ).toBe('Local Kafka (Kafka)');
    });
    await waitFor(() => {
      expect(
        within(dialog).getByRole<HTMLInputElement>('combobox', { name: 'Environment' }).value,
      ).toBe('staging');
    });
    await user.click(within(dialog).getByRole('button', { name: 'Preview' }));
    expect(await within(dialog).findByText('orders-stg')).toBeDefined();
    const copies = within(dialog).getByRole('textbox', { name: 'Copies' });
    await user.clear(copies);
    await user.type(copies, '2');
    await user.click(within(dialog).getByRole('button', { name: 'Send' }));

    expect(await within(dialog).findByText('2 copies sent')).toBeDefined();
    expect(resolutions).toEqual([{ environmentId: 5 }, { environmentId: 5 }]);
    expect(produced).toEqual([
      {
        connectionId: 1,
        destination: { type: 'TOPIC', name: 'orders-stg' },
        key: '42',
        headers: { 'trace-id': 'abc' },
        payload: { format: 'JSON', content: '{"id": "42"}' },
        copies: 2,
      },
    ]);
  });

  it('lists the variables an environment is missing', async () => {
    const user = userEvent.setup();
    serveCollections([aCollection({ id: 1 })]);
    server.use(
      http.post('/api/v1/collections/1/templates/10/resolution', () =>
        HttpResponse.json(
          {
            title: 'Unresolved variables',
            status: 422,
            detail: 'No value for {{env}}, {{orderId}}',
            code: 'collection.unresolved-variables',
            missing: ['env', 'orderId'],
            unknownBuiltIns: ['$guid'],
          },
          { status: 422 },
        ),
      ),
    );
    renderAppAt('/collections/1');

    await user.click(await screen.findByRole('button', { name: 'Send Order created' }));
    const dialog = screen.getByRole('dialog', { name: 'Send Order created' });
    await user.click(within(dialog).getByRole('button', { name: 'Preview' }));

    const alert = await within(dialog).findByRole('alert');
    expect(within(alert).getByText('{{env}}')).toBeDefined();
    expect(within(alert).getByText('{{orderId}}')).toBeDefined();
    expect(within(alert).getByText('{{$guid}}')).toBeDefined();
    expect(within(alert).getByText(/Choose an environment/)).toBeDefined();
  });

  it('says when no connection can send a template', async () => {
    const user = userEvent.setup();
    serveCollections([aCollection({ id: 1, templates: [aTemplate({ brokerType: 'PULSAR' })] })]);
    renderAppAt('/collections/1');

    await user.click(await screen.findByRole('button', { name: 'Send Order created' }));

    expect(await screen.findByText(/No Pulsar connection yet/)).toBeDefined();
  });
});
