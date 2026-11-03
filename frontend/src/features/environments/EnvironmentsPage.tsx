import {
  Alert,
  Button,
  Center,
  Group,
  Loader,
  Stack,
  Table,
  Text,
  Title,
  VisuallyHidden,
} from '@mantine/core';
import { useState } from 'react';
import type { EnvironmentResponse } from '../../api/collection';
import { ApiProblem } from '../../api/problem';
import { useEnvironments } from '../../shared/environments';
import { DeleteEnvironmentModal } from './DeleteEnvironmentModal';
import { EnvironmentFormModal } from './EnvironmentFormModal';

type Dialog =
  | { kind: 'closed' }
  | { kind: 'create' }
  | { kind: 'edit'; environment: EnvironmentResponse }
  | { kind: 'delete'; environment: EnvironmentResponse };

function variableCount(environment: EnvironmentResponse): string {
  const count = environment.variables.length;
  if (count === 0) {
    return 'No variables';
  }
  return count === 1 ? '1 variable' : `${String(count)} variables`;
}

export function EnvironmentsPage() {
  const environments = useEnvironments();
  const [dialog, setDialog] = useState<Dialog>({ kind: 'closed' });
  const close = () => {
    setDialog({ kind: 'closed' });
  };

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Environments</Title>
        <Button
          onClick={() => {
            setDialog({ kind: 'create' });
          }}
        >
          New environment
        </Button>
      </Group>
      <Text c="dimmed" size="sm">
        Each environment gives values to the {'{{variables}}'} of templates. Choose the active one
        in the header.
      </Text>
      {environments.isPending && (
        <Center py="xl">
          <Loader aria-label="Loading environments" />
        </Center>
      )}
      {environments.isError && (
        <Alert color="red" title="The environments could not be loaded">
          {environments.error instanceof ApiProblem
            ? environments.error.message
            : 'TopicMan could not be reached. Check that it is running.'}
        </Alert>
      )}
      {environments.isSuccess && environments.data.length === 0 && (
        <Text c="dimmed">
          No environments yet. Create one to give values to template variables.
        </Text>
      )}
      {environments.isSuccess && environments.data.length > 0 && (
        <Table highlightOnHover verticalSpacing="sm">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Name</Table.Th>
              <Table.Th>Variables</Table.Th>
              <Table.Th>
                <VisuallyHidden>Actions</VisuallyHidden>
              </Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {environments.data.map((environment) => (
              <Table.Tr key={environment.id}>
                <Table.Td>{environment.name}</Table.Td>
                <Table.Td>{variableCount(environment)}</Table.Td>
                <Table.Td>
                  <Group gap="xs" justify="flex-end" wrap="nowrap">
                    <Button
                      size="xs"
                      variant="default"
                      aria-label={`Edit ${environment.name}`}
                      onClick={() => {
                        setDialog({ kind: 'edit', environment });
                      }}
                    >
                      Edit
                    </Button>
                    <Button
                      size="xs"
                      variant="subtle"
                      color="red"
                      aria-label={`Delete ${environment.name}`}
                      onClick={() => {
                        setDialog({ kind: 'delete', environment });
                      }}
                    >
                      Delete
                    </Button>
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
      {dialog.kind === 'create' && <EnvironmentFormModal environment={undefined} onClose={close} />}
      {dialog.kind === 'edit' && (
        <EnvironmentFormModal environment={dialog.environment} onClose={close} />
      )}
      {dialog.kind === 'delete' && (
        <DeleteEnvironmentModal environment={dialog.environment} onClose={close} />
      )}
    </Stack>
  );
}
