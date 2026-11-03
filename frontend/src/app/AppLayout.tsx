import { AppShell, Burger, Group, NavLink, Title } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { NavLink as RouterNavLink, Outlet } from 'react-router';
import { ColorSchemeToggle } from './ColorSchemeToggle';
import { EnvironmentPicker } from './EnvironmentPicker';

const NAVIGATION = [
  { label: 'Connections', to: '/connections' },
  { label: 'Produce', to: '/produce' },
  { label: 'Consume', to: '/consume' },
  { label: 'Collections', to: '/collections' },
  { label: 'Environments', to: '/environments' },
];

export function AppLayout() {
  const [navigationOpened, { toggle: toggleNavigation, close: closeNavigation }] = useDisclosure();

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 220, breakpoint: 'sm', collapsed: { mobile: !navigationOpened } }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group>
            <Burger
              opened={navigationOpened}
              onClick={toggleNavigation}
              hiddenFrom="sm"
              size="sm"
              aria-label="Toggle navigation"
            />
            <Title order={1} size="h3">
              TopicMan
            </Title>
          </Group>
          <Group gap="md">
            <EnvironmentPicker />
            <ColorSchemeToggle />
          </Group>
        </Group>
      </AppShell.Header>
      <AppShell.Navbar p="xs" aria-label="Main">
        {NAVIGATION.map(({ label, to }) => (
          <NavLink
            key={to}
            component={RouterNavLink}
            to={to}
            label={label}
            onClick={closeNavigation}
          />
        ))}
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
