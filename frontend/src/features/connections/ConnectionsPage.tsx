import { Alert, Button, Center, Group, Loader, Stack, Text, Title } from '@mantine/core';
import { useState } from 'react';
import type { ConnectionResponse } from '../../api/connection';
import { ApiProblem } from '../../api/problem';
import { ConnectionFormModal } from './ConnectionFormModal';
import { ConnectionsTable } from './ConnectionsTable';
import { useConnections } from './connectionQueries';
import { DeleteConnectionModal } from './DeleteConnectionModal';

type Dialog =
  | { kind: 'closed' }
  | { kind: 'create' }
  | { kind: 'edit'; connection: ConnectionResponse }
  | { kind: 'delete'; connection: ConnectionResponse };

export function ConnectionsPage() {
  const connections = useConnections();
  const [dialog, setDialog] = useState<Dialog>({ kind: 'closed' });
  const close = () => {
    setDialog({ kind: 'closed' });
  };

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Connections</Title>
        <Button
          onClick={() => {
            setDialog({ kind: 'create' });
          }}
        >
          New connection
        </Button>
      </Group>
      {connections.isPending && (
        <Center py="xl">
          <Loader aria-label="Loading connections" />
        </Center>
      )}
      {connections.isError && (
        <Alert color="red" title="The connections could not be loaded">
          {connections.error instanceof ApiProblem
            ? connections.error.message
            : 'TopicMan could not be reached. Check that it is running.'}
        </Alert>
      )}
      {connections.isSuccess && connections.data.length === 0 && (
        <Text c="dimmed">No connections yet. Create one to start producing and consuming.</Text>
      )}
      {connections.isSuccess && connections.data.length > 0 && (
        <ConnectionsTable
          connections={connections.data}
          onEdit={(connection) => {
            setDialog({ kind: 'edit', connection });
          }}
          onDelete={(connection) => {
            setDialog({ kind: 'delete', connection });
          }}
        />
      )}
      {dialog.kind === 'create' && <ConnectionFormModal connection={undefined} onClose={close} />}
      {dialog.kind === 'edit' && (
        <ConnectionFormModal connection={dialog.connection} onClose={close} />
      )}
      {dialog.kind === 'delete' && (
        <DeleteConnectionModal connection={dialog.connection} onClose={close} />
      )}
    </Stack>
  );
}
