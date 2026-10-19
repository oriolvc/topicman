import { Alert, Code, List, ScrollArea, Stack, Text } from '@mantine/core';
import { ApiProblem } from '../../api/problem';

export type ProduceOutcome =
  { kind: 'sent'; receipts: string[] } | { kind: 'failed'; error: unknown; copies: number };

type ProduceResultProps = {
  outcome: ProduceOutcome;
};

export function ProduceResult({ outcome }: ProduceResultProps) {
  if (outcome.kind === 'sent') {
    const { receipts } = outcome;
    return (
      <Alert
        color="green"
        title={receipts.length === 1 ? 'Message sent' : `${String(receipts.length)} copies sent`}
      >
        <Stack gap="xs">
          <Text size="sm">The broker acknowledged:</Text>
          <ScrollArea.Autosize mah={240}>
            <List listStyleType="none" spacing={4} size="sm" aria-label="Acknowledgements">
              {receipts.map((receipt, index) => (
                <List.Item key={index}>
                  <Code>{receipt}</Code>
                </List.Item>
              ))}
            </List>
          </ScrollArea.Autosize>
        </Stack>
      </Alert>
    );
  }
  const { error, copies } = outcome;
  if (!(error instanceof ApiProblem)) {
    return (
      <Alert color="red" title="The message could not be sent">
        TopicMan could not be reached. Check that it is running.
      </Alert>
    );
  }
  const sent = error.problem['sent'];
  return (
    <Alert color="red" title={error.problem.title ?? 'The message could not be sent'}>
      <Stack gap="xs">
        <Text size="sm">{error.message}</Text>
        {typeof sent === 'number' && copies > 1 && (
          <Text size="sm">
            {sent} of {copies} copies were sent before the failure.
          </Text>
        )}
      </Stack>
    </Alert>
  );
}
