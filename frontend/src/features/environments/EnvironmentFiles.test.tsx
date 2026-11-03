import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { anEnvironment } from '../../test/environmentFixtures';
import { renderAppAt } from '../../test/renderWithProviders';
import { server } from '../../test/server';

const FILE = 'topicman: 1\nkind: environment\nname: staging\nvariables: []\n';

describe('Environment files', () => {
  beforeEach(() => {
    server.use(
      http.get('/api/v1/environments', () =>
        HttpResponse.json([anEnvironment({ id: 1, name: 'staging' })]),
      ),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('exports an environment as a YAML file named by the server', async () => {
    const user = userEvent.setup();
    const downloads: string[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      downloads.push(this.download);
    });
    server.use(
      http.get(
        '/api/v1/environments/1/export',
        () =>
          new HttpResponse(FILE, {
            headers: { 'Content-Disposition': 'attachment; filename="staging.topicman.yaml"' },
          }),
      ),
    );
    renderAppAt('/environments');

    await user.click(await screen.findByRole('button', { name: 'Export staging' }));

    await waitFor(() => {
      expect(downloads).toEqual(['staging.topicman.yaml']);
    });
  });

  it('imports an environment file, under another name when its own is taken', async () => {
    const user = userEvent.setup();
    const imports: { name: string | null; contentType: string | null; body: string }[] = [];
    server.use(
      http.post('/api/v1/environments/import', async ({ request }) => {
        const name = new URL(request.url).searchParams.get('name');
        imports.push({
          name,
          contentType: request.headers.get('Content-Type'),
          body: await request.text(),
        });
        return name === null
          ? HttpResponse.json(
              {
                title: 'Environment name taken',
                status: 409,
                detail: "An environment named 'staging' already exists",
                code: 'collection.environment-name-taken',
              },
              { status: 409 },
            )
          : HttpResponse.json(anEnvironment({ id: 2, name }), { status: 201 });
      }),
    );
    renderAppAt('/environments');

    await user.upload(
      await screen.findByLabelText('Environment file'),
      new File([FILE], 'staging.topicman.yaml', { type: 'application/yaml' }),
    );
    const dialog = await screen.findByRole('dialog', { name: 'Import environment' });
    await user.type(within(dialog).getByRole('textbox', { name: /Name/ }), 'staging copy');
    await user.click(within(dialog).getByRole('button', { name: 'Import' }));

    expect(await screen.findByText('Environment staging copy imported')).toBeDefined();
    expect(imports).toEqual([
      { name: null, contentType: 'application/yaml', body: FILE },
      { name: 'staging copy', contentType: 'application/yaml', body: FILE },
    ]);
  });
});
