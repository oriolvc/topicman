import '@mantine/core/styles.css';

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AppProviders } from './app/AppProviders';
import { App } from './app/App';
import { createQueryClient } from './app/queryClient';

const root = document.getElementById('root');
if (root === null) {
  throw new Error('The page has no #root element');
}

createRoot(root).render(
  <StrictMode>
    <AppProviders queryClient={createQueryClient()}>
      <App />
    </AppProviders>
  </StrictMode>,
);
