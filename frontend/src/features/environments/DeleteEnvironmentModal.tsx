import { Button, Group, Modal, Stack, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import type { EnvironmentResponse } from '../../api/collection';
import { showProblem } from '../../shared/showProblem';
import { useDeleteEnvironment } from './environmentQueries';

type DeleteEnvironmentModalProps = {
  environment: EnvironmentResponse;
  onClose: () => void;
};

export function DeleteEnvironmentModal({ environment, onClose }: DeleteEnvironmentModalProps) {
  const remove = useDeleteEnvironment();

  const confirm = () => {
    remove.mutate(environment.id, {
      onSuccess: () => {
        notifications.show({ color: 'green', message: `Environment ${environment.name} deleted` });
        onClose();
      },
      onError: (error) => {
        showProblem(error, 'The environment could not be deleted');
      },
    });
  };

  return (
    <Modal opened onClose={onClose} title="Delete environment">
      <Stack>
        <Text>
          Delete <strong>{environment.name}</strong> and its variables? Templates are not changed.
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
