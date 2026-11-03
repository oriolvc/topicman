import { Button, Group, Modal, Stack, Text } from '@mantine/core';
import type { ReactNode } from 'react';

type ConfirmDeleteModalProps = {
  title: string;
  children: ReactNode;
  deleting: boolean;
  onConfirm: () => void;
  onClose: () => void;
};

export function ConfirmDeleteModal({
  title,
  children,
  deleting,
  onConfirm,
  onClose,
}: ConfirmDeleteModalProps) {
  return (
    <Modal opened onClose={onClose} title={title}>
      <Stack>
        <Text>{children}</Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            Cancel
          </Button>
          <Button color="red" loading={deleting} onClick={onConfirm}>
            Delete
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
