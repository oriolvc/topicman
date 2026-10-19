import { Badge, Button, Group, Table, Text, VisuallyHidden } from '@mantine/core';
import type { ConnectionResponse } from '../../api/connection';
import { BROKER_LABELS } from '../../shared/brokers';
import { CREDENTIALS_LABELS } from './brokers';

type ConnectionsTableProps = {
  connections: ConnectionResponse[];
  onEdit: (connection: ConnectionResponse) => void;
  onDelete: (connection: ConnectionResponse) => void;
};

export function ConnectionsTable({ connections, onEdit, onDelete }: ConnectionsTableProps) {
  return (
    <Table.ScrollContainer minWidth={640}>
      <Table highlightOnHover verticalSpacing="sm">
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Name</Table.Th>
            <Table.Th>Broker</Table.Th>
            <Table.Th>Address</Table.Th>
            <Table.Th>Credentials</Table.Th>
            <Table.Th>
              <VisuallyHidden>Actions</VisuallyHidden>
            </Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {connections.map((connection) => (
            <Table.Tr key={connection.id}>
              <Table.Td>{connection.name}</Table.Td>
              <Table.Td>
                <Badge variant="light">{BROKER_LABELS[connection.brokerType]}</Badge>
              </Table.Td>
              <Table.Td>
                <Text ff="monospace" size="sm">
                  {connection.address}
                </Text>
              </Table.Td>
              <Table.Td>{CREDENTIALS_LABELS[connection.credentialsType]}</Table.Td>
              <Table.Td>
                <Group gap="xs" justify="flex-end" wrap="nowrap">
                  <Button
                    size="xs"
                    variant="default"
                    aria-label={`Edit ${connection.name}`}
                    onClick={() => {
                      onEdit(connection);
                    }}
                  >
                    Edit
                  </Button>
                  <Button
                    size="xs"
                    variant="subtle"
                    color="red"
                    aria-label={`Delete ${connection.name}`}
                    onClick={() => {
                      onDelete(connection);
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
    </Table.ScrollContainer>
  );
}
