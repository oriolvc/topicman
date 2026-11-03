import { Alert, Code, List, Stack, Table, Text } from '@mantine/core';
import type { ResolvedMessageResponse } from '../../api/collection';
import { ApiProblem } from '../../api/problem';
import { describeDestination } from './templateText';

export type Resolution =
  { kind: 'resolved'; message: ResolvedMessageResponse } | { kind: 'failed'; error: unknown };

type ResolutionPreviewProps = {
  resolution: Resolution;
  /** The environment the template was resolved against, if any. */
  environmentName: string | undefined;
};

function namesOf(problem: Record<string, unknown>, property: string): string[] {
  const names = problem[property];
  return Array.isArray(names)
    ? names.filter((name): name is string => typeof name === 'string')
    : [];
}

/** The resolved message to produce, or why its placeholders could not all be resolved. */
export function ResolutionPreview({ resolution, environmentName }: ResolutionPreviewProps) {
  if (resolution.kind === 'resolved') {
    const { message } = resolution;
    const headers = Object.entries(message.headers);
    return (
      <Stack gap="xs" aria-label="Resolved message">
        <Table withRowBorders={false} verticalSpacing={2}>
          <Table.Tbody>
            <Table.Tr>
              <Table.Th w={110}>Destination</Table.Th>
              <Table.Td>
                <Code>{describeDestination(message.destination)}</Code>
              </Table.Td>
            </Table.Tr>
            {message.key !== undefined && (
              <Table.Tr>
                <Table.Th>Key</Table.Th>
                <Table.Td>
                  <Code>{message.key}</Code>
                </Table.Td>
              </Table.Tr>
            )}
            {headers.length > 0 && (
              <Table.Tr>
                <Table.Th>Headers</Table.Th>
                <Table.Td>
                  {headers.map(([name, value]) => (
                    <Text key={name} size="sm" ff="monospace">
                      {name}: {value}
                    </Text>
                  ))}
                </Table.Td>
              </Table.Tr>
            )}
          </Table.Tbody>
        </Table>
        <Code block>{message.payload.content}</Code>
      </Stack>
    );
  }
  const { error } = resolution;
  if (error instanceof ApiProblem && error.code === 'collection.unresolved-variables') {
    const missing = namesOf(error.problem, 'missing');
    const unknown = namesOf(error.problem, 'unknownBuiltIns');
    return (
      <Alert color="orange" title="Some placeholders have no value">
        <Stack gap="xs">
          <List size="sm">
            {missing.map((name) => (
              <List.Item key={name}>
                <Code>{`{{${name}}}`}</Code>
              </List.Item>
            ))}
            {unknown.map((name) => (
              <List.Item key={name}>
                <Code>{`{{${name}}}`}</Code> is not a built-in variable
              </List.Item>
            ))}
          </List>
          {missing.length > 0 && (
            <Text size="sm">
              {environmentName === undefined
                ? 'Choose an environment that defines them.'
                : `Add them to the ${environmentName} environment.`}
            </Text>
          )}
        </Stack>
      </Alert>
    );
  }
  return (
    <Alert color="red" title="The template could not be resolved">
      {error instanceof ApiProblem
        ? error.message
        : 'TopicMan could not be reached. Check that it is running.'}
    </Alert>
  );
}
