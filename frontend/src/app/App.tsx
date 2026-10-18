import { AppShell, Group, Text, Title } from '@mantine/core';
import { ColorSchemeToggle } from './ColorSchemeToggle';

export function App() {
  return (
    <AppShell header={{ height: 60 }} padding="md">
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Title order={1} size="h3">
            TopicMan
          </Title>
          <ColorSchemeToggle />
        </Group>
      </AppShell.Header>
      <AppShell.Main>
        <Text c="dimmed">Produce and consume test messages for Pulsar, Kafka and RabbitMQ.</Text>
      </AppShell.Main>
    </AppShell>
  );
}
