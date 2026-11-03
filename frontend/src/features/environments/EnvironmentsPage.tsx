import {
  Alert,
  Button,
  Center,
  FileButton,
  Group,
  Loader,
  Stack,
  Table,
  Text,
  Title,
  VisuallyHidden,
} from '@mantine/core';
import { useRef, useState } from 'react';
import { notifications } from '@mantine/notifications';
import type { EnvironmentResponse } from '../../api/collection';
import { ApiProblem } from '../../api/problem';
import { useEnvironments } from '../../shared/environments';
import { ImportNameModal } from '../../shared/ImportNameModal';
import { showProblem } from '../../shared/showProblem';
import { useExportEnvironment, useImportEnvironment } from './environmentQueries';
import { DeleteEnvironmentModal } from './DeleteEnvironmentModal';
import { EnvironmentFormModal } from './EnvironmentFormModal';

type Dialog =
  | { kind: 'closed' }
  | { kind: 'create' }
  | { kind: 'importName'; text: string; reason: string }
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
  const importEnvironment = useImportEnvironment();
  const exportEnvironment = useExportEnvironment();
  const resetFileInput = useRef<() => void>(null);

  const imported = (created: EnvironmentResponse) => {
    notifications.show({ color: 'green', message: `Environment ${created.name} imported` });
    close();
  };

  const importFile = async (file: File | null) => {
    resetFileInput.current?.();
    if (file === null) {
      return;
    }
    const text = await file.text();
    try {
      imported(await importEnvironment.mutateAsync({ text }));
    } catch (error) {
      if (error instanceof ApiProblem && error.code === 'collection.environment-name-taken') {
        setDialog({ kind: 'importName', text, reason: error.message });
      } else {
        showProblem(error, 'The file could not be imported');
      }
    }
  };

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Environments</Title>
        <Group gap="xs">
          <FileButton
            onChange={(file) => void importFile(file)}
            accept=".yaml,.yml,application/yaml"
            resetRef={resetFileInput}
            inputProps={{ 'aria-label': 'Environment file' }}
          >
            {(props) => (
              <Button variant="default" loading={importEnvironment.isPending} {...props}>
                Import
              </Button>
            )}
          </FileButton>
          <Button
            onClick={() => {
              setDialog({ kind: 'create' });
            }}
          >
            New environment
          </Button>
        </Group>
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
                      aria-label={`Export ${environment.name}`}
                      onClick={() => {
                        exportEnvironment.mutate(environment, {
                          onError: (error) => {
                            showProblem(error, 'The environment could not be exported');
                          },
                        });
                      }}
                    >
                      Export
                    </Button>
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
      {dialog.kind === 'importName' && (
        <ImportNameModal
          title="Import environment"
          reason={dialog.reason}
          saving={importEnvironment.isPending}
          onClose={close}
          onSubmit={async (name) => {
            try {
              imported(await importEnvironment.mutateAsync({ text: dialog.text, name }));
            } catch (error) {
              if (
                error instanceof ApiProblem &&
                error.code === 'collection.environment-name-taken'
              ) {
                throw error;
              }
              showProblem(error, 'The file could not be imported');
              close();
            }
          }}
        />
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
