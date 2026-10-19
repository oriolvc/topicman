import { Badge, Code, Group, Paper, Stack, Text } from '@mantine/core';
import { formatPayload, type ConsumedMessage } from './consumedMessages';

type ConsumedMessageCardProps = {
  message: ConsumedMessage;
  raw: boolean;
};

export function ConsumedMessageCard({ message, raw }: ConsumedMessageCardProps) {
  const headers = Object.entries(message.headers);
  return (
    <Paper withBorder p="sm" component="article" aria-label={`Message at ${message.position}`}>
      <Stack gap="xs">
        <Group gap="xs">
          <Code>{message.position}</Code>
          <Text size="xs" c="dimmed">
            {message.timestamp}
          </Text>
          {message.key !== undefined && (
            <Badge variant="light" tt="none">
              key: {message.key}
            </Badge>
          )}
          <Badge variant="outline" color="gray">
            {message.payload.format}
          </Badge>
        </Group>
        {headers.length > 0 && (
          <Group gap={4} aria-label="Headers">
            {headers.map(([name, value]) => (
              <Code key={name}>
                {name}: {value}
              </Code>
            ))}
          </Group>
        )}
        <Code block aria-label="Payload">
          {formatPayload(message.payload, raw)}
        </Code>
      </Stack>
    </Paper>
  );
}
