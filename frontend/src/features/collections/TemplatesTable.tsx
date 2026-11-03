import { Anchor, Badge, Button, Group, Table, Text, VisuallyHidden } from '@mantine/core';
import { Link } from 'react-router';
import type { TemplateResponse } from '../../api/collection';
import { BROKER_LABELS } from '../../shared/brokers';
import { describeDestination } from './templateText';

type TemplatesTableProps = {
  collectionId: number;
  templates: TemplateResponse[];
  onSend: (template: TemplateResponse) => void;
  onDelete: (template: TemplateResponse) => void;
};

export function TemplatesTable({ collectionId, templates, onSend, onDelete }: TemplatesTableProps) {
  return (
    <Table.ScrollContainer minWidth={560}>
      <Table highlightOnHover verticalSpacing="sm">
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Name</Table.Th>
            <Table.Th>Broker</Table.Th>
            <Table.Th>Destination</Table.Th>
            <Table.Th>
              <VisuallyHidden>Actions</VisuallyHidden>
            </Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {templates.map((template) => (
            <Table.Tr key={template.id}>
              <Table.Td>{template.name}</Table.Td>
              <Table.Td>
                <Badge variant="light">{BROKER_LABELS[template.brokerType]}</Badge>
              </Table.Td>
              <Table.Td>
                <Text ff="monospace" size="sm">
                  {describeDestination(template.destination)}
                </Text>
              </Table.Td>
              <Table.Td>
                <Group gap="xs" justify="flex-end" wrap="nowrap">
                  <Button
                    size="xs"
                    aria-label={`Send ${template.name}`}
                    onClick={() => {
                      onSend(template);
                    }}
                  >
                    Send
                  </Button>
                  <Anchor
                    component={Link}
                    size="sm"
                    to={`/produce?collection=${String(collectionId)}&template=${String(template.id)}`}
                    aria-label={`Edit ${template.name}`}
                  >
                    Edit
                  </Anchor>
                  <Button
                    size="xs"
                    variant="subtle"
                    color="red"
                    aria-label={`Delete ${template.name}`}
                    onClick={() => {
                      onDelete(template);
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
