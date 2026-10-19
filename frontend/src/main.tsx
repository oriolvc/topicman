import '@mantine/core/styles.css';
import '@mantine/notifications/styles.css';

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AppProviders } from './app/AppProviders';
import { createQueryClient } from './app/queryClient';
import { routes } from './app/routes';

const root = document.getElementById('root');
if (root === null) {
  throw new Error('The page has no #root element');
}

createRoot(root).render(
  <StrictMode>
    <AppProviders queryClient={createQueryClient()}>
      <RouterProvider router={createBrowserRouter(routes)} />
    </AppProviders>
  </StrictMode>,
);
