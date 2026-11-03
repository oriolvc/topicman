import { Button, Group, Modal, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { ApiProblem } from '../../api/problem';
import { showProblem } from '../../shared/showProblem';

type CollectionNameModalProps = {
  title: string;
  submitLabel: string;
  initialName: string;
  saving: boolean;
  /** Saves the trimmed name; a rejected promise keeps the dialog open. */
  onSubmit: (name: string) => Promise<unknown>;
  onClose: () => void;
};

/** Asks for a collection's name, to create or rename one. */
export function CollectionNameModal({
  title,
  submitLabel,
  initialName,
  saving,
  onSubmit,
  onClose,
}: CollectionNameModalProps) {
  const form = useForm({
    mode: 'controlled',
    initialValues: { name: initialName },
    validate: { name: (name) => (name.trim() === '' ? 'Enter a name' : null) },
  });

  const submit = async ({ name }: { name: string }) => {
    try {
      await onSubmit(name.trim());
    } catch (error) {
      if (error instanceof ApiProblem && error.code === 'collection.name-taken') {
        form.setFieldError('name', 'Another collection already has this name');
      } else {
        showProblem(error, 'The collection could not be saved');
      }
    }
  };

  return (
    <Modal opened onClose={onClose} title={title}>
      <form onSubmit={form.onSubmit((values) => void submit(values))} noValidate>
        <Stack>
          <TextInput label="Name" withAsterisk data-autofocus {...form.getInputProps('name')} />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" loading={saving}>
              {submitLabel}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
