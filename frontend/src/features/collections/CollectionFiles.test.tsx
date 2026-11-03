import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { aCollection, summaryOf } from '../../test/collectionFixtures';
import { renderAppAt } from '../../test/renderWithProviders';
import { server } from '../../test/server';

const FILE = 'topicman: 1\nkind: collection\nname: Orders\ntemplates: []\n';

/** Records the downloads the screen starts, by file name. */
function captureDownloads() {
  const names: string[] = [];
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
    this: HTMLAnchorElement,
  ) {
    names.push(this.download);
  });
  return names;
}

describe('Collection files', () => {
  const orders = aCollection({ id: 1, name: 'Orders' });

  beforeEach(() => {
    server.use(
      http.get('/api/v1/collections', () => HttpResponse.json([summaryOf(orders)])),
      http.get('/api/v1/collections/:collectionId', () => HttpResponse.json(orders)),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('exports a collection as a YAML file named by the server', async () => {
    const user = userEvent.setup();
    const downloads = captureDownloads();
    server.use(
      http.get(
        '/api/v1/collections/1/export',
        () =>
          new HttpResponse(FILE, {
            headers: {
              'Content-Type': 'application/yaml',
              'Content-Disposition': 'attachment; filename="orders.topicman.yaml"',
            },
          }),
      ),
    );
    renderAppAt('/collections/1');

    await user.click(await screen.findByRole('button', { name: 'Export' }));

    await waitFor(() => {
      expect(downloads).toEqual(['orders.topicman.yaml']);
    });
  });

  it('imports a collection file and opens the new collection', async () => {
    const user = userEvent.setup();
    const imports: { contentType: string | null; body: string }[] = [];
    server.use(
      http.post('/api/v1/collections/import', async ({ request }) => {
        imports.push({
          contentType: request.headers.get('Content-Type'),
          body: await request.text(),
        });
        return HttpResponse.json(aCollection({ id: 7, name: 'Orders' }), { status: 201 });
      }),
    );
    const router = renderAppAt('/collections');

    await user.upload(
      await screen.findByLabelText('Collection file'),
      new File([FILE], 'orders.topicman.yaml', { type: 'application/yaml' }),
    );

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collections/7');
    });
    expect(await screen.findByText('Collection Orders imported')).toBeDefined();
    expect(imports).toEqual([{ contentType: 'application/yaml', body: FILE }]);
  });

  it('asks for another name when the imported name is taken', async () => {
    const user = userEvent.setup();
    const names: (string | null)[] = [];
    server.use(
      http.post('/api/v1/collections/import', ({ request }) => {
        const name = new URL(request.url).searchParams.get('name');
        names.push(name);
        return name === null
          ? HttpResponse.json(
              {
                title: 'Collection name taken',
                status: 409,
                detail: "A collection named 'Orders' already exists",
                code: 'collection.name-taken',
              },
              { status: 409 },
            )
          : HttpResponse.json(aCollection({ id: 8, name }), { status: 201 });
      }),
    );
    const router = renderAppAt('/collections');

    await user.upload(
      await screen.findByLabelText('Collection file'),
      new File([FILE], 'orders.topicman.yaml', { type: 'application/yaml' }),
    );
    const dialog = await screen.findByRole('dialog', { name: 'Import collection' });
    expect(within(dialog).getByText("A collection named 'Orders' already exists")).toBeDefined();
    await user.type(within(dialog).getByRole('textbox', { name: /Name/ }), 'Orders copy');
    await user.click(within(dialog).getByRole('button', { name: 'Import' }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collections/8');
    });
    expect(names).toEqual([null, 'Orders copy']);
  });

  it('shows why a file could not be imported', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('/api/v1/collections/import', () =>
        HttpResponse.json(
          {
            title: 'Invalid template',
            status: 400,
            detail: "The file has more than one template named 'Order created'",
            code: 'collection.invalid-template',
          },
          { status: 400 },
        ),
      ),
    );
    renderAppAt('/collections');

    await user.upload(
      await screen.findByLabelText('Collection file'),
      new File([FILE], 'orders.topicman.yaml', { type: 'application/yaml' }),
    );

    expect(
      await screen.findByText("The file has more than one template named 'Order created'"),
    ).toBeDefined();
  });
});
