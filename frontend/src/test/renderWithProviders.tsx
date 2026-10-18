import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { AppProviders } from '../app/AppProviders';
import { createQueryClient } from '../app/queryClient';

/** Renders with the app's providers and a fresh query cache, so tests never share server state. */
export function renderWithProviders(ui: ReactElement) {
  return render(<AppProviders queryClient={createQueryClient()}>{ui}</AppProviders>);
}
