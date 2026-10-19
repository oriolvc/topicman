import { MantineProvider } from '@mantine/core';
import { Notifications } from '@mantine/notifications';
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { theme } from './theme';

type AppProvidersProps = {
  queryClient: QueryClient;
  /** `test` turns off Mantine's transitions and portals, so tests see changes right away. */
  env?: 'default' | 'test';
  children: ReactNode;
};

export function AppProviders({ queryClient, env = 'default', children }: AppProvidersProps) {
  return (
    <QueryClientProvider client={queryClient}>
      <MantineProvider theme={theme} defaultColorScheme="auto" env={env}>
        <Notifications />
        {children}
      </MantineProvider>
    </QueryClientProvider>
  );
}
