import { Button, Group, Modal, Stack, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';

type ImportNameModalProps = {
  title: string;
  /** Why another name is needed, such as the problem the import got. */
  reason: string;
  saving: boolean;
  /** Imports again under the trimmed name; a rejected promise keeps the dialog open. */
  onSubmit: (name: string) => Promise<unknown>;
  onClose: () => void;
};

/** Asks for another name when an imported file's name is taken. */
export function ImportNameModal({
  title,
  reason,
  saving,
  onSubmit,
  onClose,
}: ImportNameModalProps) {
  const form = useForm({
    mode: 'controlled',
    initialValues: { name: '' },
    validate: { name: (name) => (name.trim() === '' ? 'Enter a name' : null) },
  });

  return (
    <Modal opened onClose={onClose} title={title}>
      <form
        onSubmit={form.onSubmit(({ name }) => {
          onSubmit(name.trim()).catch(() => {
            form.setFieldError('name', 'This name is taken too');
          });
        })}
        noValidate
      >
        <Stack>
          <Text size="sm">{reason}</Text>
          <TextInput label="Name" withAsterisk data-autofocus {...form.getInputProps('name')} />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" loading={saving}>
              Import
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
