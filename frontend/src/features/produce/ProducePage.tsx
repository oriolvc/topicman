import {
  Alert,
  Anchor,
  Button,
  Grid,
  Group,
  Input,
  JsonInput,
  NumberInput,
  Paper,
  SegmentedControl,
  Select,
  Stack,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import type { Payload } from '../../api/messaging';
import { BROKER_LABELS, DESTINATION_TYPES } from '../../shared/brokers';
import { useConnections } from '../../shared/connections';
import { HeadersInput } from './HeadersInput';
import {
  initialProduceValues,
  MAX_COPIES,
  supportsMessageKey,
  toProduceRequest,
  validateProduce,
  type ProduceFormValues,
} from './produceForm';
import { useProduceMessage } from './produceQueries';
import { ProduceResult, type ProduceOutcome } from './ProduceResult';

/** The selected connection is kept in the URL (`?connection=<id>`), so it can be bookmarked. */
const CONNECTION_PARAM = 'connection';

export function ProducePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const connections = useConnections();
  const produce = useProduceMessage();
  const [outcome, setOutcome] = useState<ProduceOutcome | undefined>(undefined);
  const form = useForm<ProduceFormValues>({
    mode: 'controlled',
    initialValues: initialProduceValues(searchParams.get(CONNECTION_PARAM)),
    validate: (values) => validateProduce(values, brokerOf(values.connectionId)),
  });

  const savedConnections = connections.data ?? [];
  const brokerOf = (connectionId: string | null) =>
    savedConnections.find(({ id }) => id === connectionId)?.brokerType;
  const brokerType = brokerOf(form.values.connectionId);
  const destinationType = brokerType === undefined ? 'TOPIC' : DESTINATION_TYPES[brokerType];

  const chooseConnection = (connectionId: string | null) => {
    form.setFieldValue('connectionId', connectionId);
    setSearchParams(connectionId === null ? {} : { [CONNECTION_PARAM]: connectionId }, {
      replace: true,
    });
  };

  const send = (values: ProduceFormValues) => {
    const broker = brokerOf(values.connectionId);
    if (values.connectionId === null || broker === undefined) {
      return;
    }
    const request = toProduceRequest(values, values.connectionId, broker);
    produce.mutate(request, {
      onSuccess: ({ receipts }) => {
        setOutcome({ kind: 'sent', receipts });
      },
      onError: (error) => {
        setOutcome({ kind: 'failed', error, copies: request.copies ?? 1 });
      },
    });
  };

  if (connections.isSuccess && savedConnections.length === 0) {
    return (
      <Stack>
        <Title order={2}>Produce</Title>
        <Alert title="No connections yet">
          <Anchor component={Link} to="/connections">
            Create a connection
          </Anchor>{' '}
          to produce messages.
        </Alert>
      </Stack>
    );
  }

  return (
    <Stack>
      <Title order={2}>Produce</Title>
      <Grid>
        <Grid.Col span={{ base: 12, md: 7 }}>
          <Paper withBorder p="md">
            <form onSubmit={form.onSubmit(send)} noValidate aria-label="Produce a message">
              <Stack>
                <Select
                  label="Connection"
                  withAsterisk
                  placeholder={connections.isPending ? 'Loading connections…' : 'Choose one'}
                  data={savedConnections.map((connection) => ({
                    value: connection.id,
                    label: `${connection.name} (${BROKER_LABELS[connection.brokerType]})`,
                  }))}
                  {...form.getInputProps('connectionId')}
                  onChange={chooseConnection}
                />
                {destinationType === 'TOPIC' ? (
                  <TextInput
                    label="Topic"
                    withAsterisk
                    {...form.getInputProps('destinationName')}
                  />
                ) : (
                  <Group grow align="flex-start">
                    <TextInput
                      label="Exchange"
                      withAsterisk
                      {...form.getInputProps('destinationName')}
                    />
                    <TextInput
                      label="Routing key"
                      description="Empty for fanout exchanges"
                      {...form.getInputProps('routingKey')}
                    />
                  </Group>
                )}
                {(brokerType === undefined || supportsMessageKey(brokerType)) && (
                  <TextInput
                    label="Key"
                    description="Optional partitioning key"
                    {...form.getInputProps('messageKey')}
                  />
                )}
                <HeadersInput form={form} />
                <Input.Wrapper label="Payload format">
                  <SegmentedControl<Payload['format']>
                    fullWidth
                    value={form.values.payloadFormat}
                    onChange={(format) => {
                      form.setFieldValue('payloadFormat', format);
                    }}
                    data={[
                      { value: 'JSON', label: 'JSON' },
                      { value: 'TEXT', label: 'Text' },
                    ]}
                  />
                </Input.Wrapper>
                {form.values.payloadFormat === 'JSON' ? (
                  <JsonInput
                    label="Payload"
                    withAsterisk
                    autosize
                    minRows={6}
                    maxRows={20}
                    formatOnBlur
                    styles={{ input: { fontFamily: 'var(--mantine-font-family-monospace)' } }}
                    {...form.getInputProps('payload')}
                  />
                ) : (
                  <Textarea
                    label="Payload"
                    autosize
                    minRows={6}
                    maxRows={20}
                    {...form.getInputProps('payload')}
                  />
                )}
                <Group justify="space-between" align="flex-end">
                  <NumberInput
                    label="Copies"
                    min={1}
                    max={MAX_COPIES}
                    allowDecimal={false}
                    allowNegative={false}
                    w={120}
                    {...form.getInputProps('copies')}
                  />
                  <Button type="submit" loading={produce.isPending}>
                    Send
                  </Button>
                </Group>
              </Stack>
            </form>
          </Paper>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 5 }}>
          <section aria-label="Result">
            {outcome !== undefined && <ProduceResult outcome={outcome} />}
          </section>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
