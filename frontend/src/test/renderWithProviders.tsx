import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AppProviders } from '../app/AppProviders';
import { createQueryClient } from '../app/queryClient';
import { routes } from '../app/routes';

/** Renders with the app's providers and a fresh query cache, so tests never share server state. */
export function renderWithProviders(ui: ReactElement) {
  return render(
    <AppProviders queryClient={createQueryClient()} env="test">
      {ui}
    </AppProviders>,
  );
}

/** Renders the whole app at a URL, as a user opening a bookmark would. */
export function renderAppAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  renderWithProviders(<RouterProvider router={router} />);
  return router;
}
