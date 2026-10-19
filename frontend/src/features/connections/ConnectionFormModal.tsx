import {
  Button,
  Group,
  Input,
  Modal,
  PasswordInput,
  SegmentedControl,
  Stack,
  Text,
  TextInput,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import type { BrokerType, ConnectionResponse } from '../../api/connection';
import { ApiProblem } from '../../api/problem';
import { showProblem } from '../../shared/showProblem';
import { BROKER_TYPES, BROKERS, CREDENTIALS_LABELS } from './brokers';
import {
  credentialsAfterBrokerChange,
  credentialsChoices,
  existingConnectionValues,
  newConnectionValues,
  toCreateRequest,
  toUpdateRequest,
  validateConnection,
  type ConnectionFormValues,
} from './connectionForm';
import { useCreateConnection, useUpdateConnection } from './connectionQueries';

type ConnectionFormModalProps = {
  /** The connection to edit, or `undefined` to create one. */
  connection: ConnectionResponse | undefined;
  onClose: () => void;
};

/** Mount it only while open: it starts from the connection's values each time. */
export function ConnectionFormModal({ connection, onClose }: ConnectionFormModalProps) {
  const editing = connection !== undefined;
  const form = useForm<ConnectionFormValues>({
    mode: 'controlled',
    initialValues: editing ? existingConnectionValues(connection) : newConnectionValues(),
    validate: validateConnection,
  });
  const create = useCreateConnection();
  const update = useUpdateConnection();
  const saving = create.isPending || update.isPending;
  const { brokerType, credentialsType } = form.values;

  const handleSubmit = async (values: ConnectionFormValues) => {
    try {
      if (editing) {
        await update.mutateAsync({
          id: connection.id,
          request: toUpdateRequest(connection, values),
        });
      } else {
        await create.mutateAsync(toCreateRequest(values));
      }
    } catch (error) {
      if (error instanceof ApiProblem && error.code === 'connection.name-taken') {
        form.setFieldError('name', 'Another connection already has this name');
      } else {
        showProblem(error, 'The connection could not be saved');
      }
      return;
    }
    notifications.show({
      color: 'green',
      message: `Connection ${values.name.trim()} ${editing ? 'saved' : 'created'}`,
    });
    onClose();
  };

  const changeBroker = (next: BrokerType) => {
    form.setValues({
      brokerType: next,
      credentialsType: credentialsAfterBrokerChange(credentialsType, next),
    });
  };

  return (
    <Modal opened onClose={onClose} title={editing ? 'Edit connection' : 'New connection'}>
      <form onSubmit={form.onSubmit((values) => void handleSubmit(values))} noValidate>
        <Stack>
          <TextInput label="Name" withAsterisk data-autofocus {...form.getInputProps('name')} />
          <Input.Wrapper
            label="Broker"
            description={editing ? 'Create a new connection to use another broker.' : undefined}
          >
            <SegmentedControl
              fullWidth
              disabled={editing}
              value={brokerType}
              onChange={changeBroker}
              data={BROKER_TYPES.map((type) => ({ value: type, label: BROKERS[type].label }))}
            />
          </Input.Wrapper>
          <TextInput
            label="Address"
            withAsterisk
            placeholder={BROKERS[brokerType].addressPlaceholder}
            {...form.getInputProps('address')}
          />
          <Input.Wrapper label="Credentials">
            <SegmentedControl
              fullWidth
              value={credentialsType}
              onChange={(value) => {
                form.setFieldValue('credentialsType', value);
              }}
              data={credentialsChoices(brokerType, editing).map((choice) => ({
                value: choice,
                label: choice === 'KEEP' ? 'Keep current' : CREDENTIALS_LABELS[choice],
              }))}
            />
          </Input.Wrapper>
          {credentialsType === 'KEEP' && connection !== undefined && (
            <Text size="sm" c="dimmed">
              Current: {CREDENTIALS_LABELS[connection.credentialsType]}. Saved credentials are never
              shown.
            </Text>
          )}
          {credentialsType === 'TOKEN' && (
            <PasswordInput
              label="Token"
              withAsterisk
              autoComplete="off"
              {...form.getInputProps('token')}
            />
          )}
          {credentialsType === 'USERNAME_PASSWORD' && (
            <>
              <TextInput
                label="Username"
                withAsterisk
                autoComplete="off"
                {...form.getInputProps('username')}
              />
              <PasswordInput
                label="Password"
                withAsterisk
                autoComplete="new-password"
                {...form.getInputProps('password')}
              />
            </>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" loading={saving}>
              {editing ? 'Save' : 'Create'}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
