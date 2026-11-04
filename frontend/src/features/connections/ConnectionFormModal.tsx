import {
  Button,
  Group,
  Input,
  Modal,
  PasswordInput,
  SegmentedControl,
  Stack,
  Switch,
  Text,
  Textarea,
  TextInput,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import type { BrokerType, ConnectionResponse, SaslMechanism } from '../../api/connection';
import { ApiProblem } from '../../api/problem';
import { showProblem } from '../../shared/showProblem';
import { BROKER_LABELS, BROKER_TYPES } from '../../shared/brokers';
import {
  BROKER_SETTINGS,
  CREDENTIALS_LABELS,
  SASL_MECHANISM_LABELS,
  SASL_MECHANISMS,
} from './brokers';
import {
  credentialsAfterBrokerChange,
  credentialsChoices,
  existingConnectionValues,
  newConnectionValues,
  tlsInUse,
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
  const tlsScheme = BROKER_SETTINGS[brokerType].tlsScheme;

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
              data={BROKER_TYPES.map((type) => ({ value: type, label: BROKER_LABELS[type] }))}
            />
          </Input.Wrapper>
          <TextInput
            label="Address"
            withAsterisk
            placeholder={BROKER_SETTINGS[brokerType].addressPlaceholder}
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
              {brokerType === 'KAFKA' && (
                <Input.Wrapper label="SASL mechanism">
                  <SegmentedControl
                    fullWidth
                    value={form.values.saslMechanism}
                    onChange={(value: SaslMechanism) => {
                      form.setFieldValue('saslMechanism', value);
                    }}
                    data={SASL_MECHANISMS.map((mechanism) => ({
                      value: mechanism,
                      label: SASL_MECHANISM_LABELS[mechanism],
                    }))}
                  />
                </Input.Wrapper>
              )}
            </>
          )}
          {tlsScheme === undefined ? (
            <Switch
              label="Use TLS"
              description="The broker's certificate and hostname are always verified."
              {...form.getInputProps('tlsEnabled', { type: 'checkbox' })}
            />
          ) : (
            <Text size="sm" c="dimmed">
              For TLS, use a {tlsScheme} address. The broker&apos;s certificate and hostname are
              always verified.
            </Text>
          )}
          {tlsInUse(form.values) && (
            <Textarea
              label="Trusted certificates"
              description="CA certificates in PEM form, for a broker whose certificate a private CA issued. Leave empty to trust the default CAs."
              placeholder="-----BEGIN CERTIFICATE-----"
              autosize
              minRows={3}
              maxRows={8}
              styles={{ input: { fontFamily: 'var(--mantine-font-family-monospace)' } }}
              {...form.getInputProps('trustedCertificates')}
            />
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
