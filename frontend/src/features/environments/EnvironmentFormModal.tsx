import { Button, Group, Modal, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import type { EnvironmentResponse } from '../../api/collection';
import { ApiProblem } from '../../api/problem';
import { showProblem } from '../../shared/showProblem';
import {
  existingEnvironmentValues,
  newEnvironmentValues,
  toEnvironmentRequest,
  validateEnvironment,
  type EnvironmentFormValues,
} from './environmentForm';
import { useCreateEnvironment, useReplaceEnvironment } from './environmentQueries';
import { VariablesInput } from './VariablesInput';

type EnvironmentFormModalProps = {
  /** The environment to edit, or `undefined` to create one. */
  environment: EnvironmentResponse | undefined;
  onClose: () => void;
};

/** Mount it only while open: it starts from the environment's values each time. */
export function EnvironmentFormModal({ environment, onClose }: EnvironmentFormModalProps) {
  const editing = environment !== undefined;
  const form = useForm<EnvironmentFormValues>({
    mode: 'controlled',
    initialValues: editing ? existingEnvironmentValues(environment) : newEnvironmentValues(),
    validate: validateEnvironment,
  });
  const create = useCreateEnvironment();
  const replace = useReplaceEnvironment();

  const handleSubmit = async (values: EnvironmentFormValues) => {
    const request = toEnvironmentRequest(values);
    try {
      if (editing) {
        await replace.mutateAsync({ id: environment.id, request });
      } else {
        await create.mutateAsync(request);
      }
    } catch (error) {
      if (error instanceof ApiProblem && error.code === 'collection.environment-name-taken') {
        form.setFieldError('name', 'Another environment already has this name');
      } else {
        showProblem(error, 'The environment could not be saved');
      }
      return;
    }
    notifications.show({
      color: 'green',
      message: `Environment ${request.name} ${editing ? 'saved' : 'created'}`,
    });
    onClose();
  };

  return (
    <Modal
      opened
      onClose={onClose}
      title={editing ? 'Edit environment' : 'New environment'}
      size="lg"
    >
      <form onSubmit={form.onSubmit((values) => void handleSubmit(values))} noValidate>
        <Stack>
          <TextInput label="Name" withAsterisk {...form.getInputProps('name')} />
          <VariablesInput form={form} />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" loading={create.isPending || replace.isPending}>
              {editing ? 'Save' : 'Create'}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
