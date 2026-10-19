import { Button, Group, Modal, Stack, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import type { ConnectionResponse } from '../../api/connection';
import { showProblem } from '../../shared/showProblem';
import { useDeleteConnection } from './connectionQueries';

type DeleteConnectionModalProps = {
  connection: ConnectionResponse;
  onClose: () => void;
};

export function DeleteConnectionModal({ connection, onClose }: DeleteConnectionModalProps) {
  const remove = useDeleteConnection();

  const confirm = () => {
    remove.mutate(connection.id, {
      onSuccess: () => {
        notifications.show({ color: 'green', message: `Connection ${connection.name} deleted` });
        onClose();
      },
      onError: (error) => {
        showProblem(error, 'The connection could not be deleted');
      },
    });
  };

  return (
    <Modal opened onClose={onClose} title="Delete connection">
      <Stack>
        <Text>
          Delete <strong>{connection.name}</strong>? Its saved credentials are deleted too.
        </Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            Cancel
          </Button>
          <Button color="red" loading={remove.isPending} onClick={confirm}>
            Delete
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
