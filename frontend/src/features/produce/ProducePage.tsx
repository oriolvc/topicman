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
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import type { Payload } from '../../api/messaging';
import { useCollection, useReplaceTemplate } from '../../shared/collections';
import { showProblem } from '../../shared/showProblem';
import { notifications } from '@mantine/notifications';
import { useBrokers } from '../../shared/brokers';
import { connectionOptions, useConnections } from '../../shared/connections';
import { DestinationInputs } from '../../shared/DestinationInputs';
import { HeadersInput } from './HeadersInput';
import {
  fromTemplate,
  hasPlaceholders,
  initialProduceValues,
  MAX_COPIES,
  toProduceRequest,
  toTemplateRequest,
  validateProduce,
  validateTemplateDraft,
  type ProduceFormValues,
} from './produceForm';
import { SaveTemplateModal } from './SaveTemplateModal';
import { useProduceMessage } from '../../shared/produceMessage';
import { ProduceResult, type ProduceOutcome } from '../../shared/ProduceResult';

/** The selected connection is kept in the URL (`?connection=<id>`), so it can be bookmarked. */
const CONNECTION_PARAM = 'connection';
/** A template being edited is in the URL too (`?collection=<id>&template=<id>`). */
const COLLECTION_PARAM = 'collection';
const TEMPLATE_PARAM = 'template';

function idParam(value: string | null): number | undefined {
  const id = Number(value);
  return value === null || !Number.isInteger(id) || id < 1 ? undefined : id;
}

export function ProducePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const connections = useConnections();
  const brokers = useBrokers();
  const produce = useProduceMessage();
  const [outcome, setOutcome] = useState<ProduceOutcome | undefined>(undefined);
  const [saving, setSaving] = useState(false);
  const replaceTemplate = useReplaceTemplate();
  const collectionId = idParam(searchParams.get(COLLECTION_PARAM));
  const templateId = idParam(searchParams.get(TEMPLATE_PARAM));
  const collection = useCollection(templateId === undefined ? undefined : collectionId);
  const template = collection.data?.templates.find(({ id }) => id === templateId);
  const form = useForm<ProduceFormValues>({
    mode: 'controlled',
    initialValues: initialProduceValues(searchParams.get(CONNECTION_PARAM)),
    validate: (values) => validateProduce(values, brokerOf(values.connectionId)),
  });

  const savedConnections = connections.data ?? [];
  const brokerOf = (connectionId: string | null) => {
    const brokerType = savedConnections.find(({ id }) => String(id) === connectionId)?.brokerType;
    return brokers.data?.find(({ type }) => type === brokerType);
  };
  const broker = brokerOf(form.values.connectionId);
  const destinationType = broker?.destinationType ?? 'TOPIC';

  // Keeps the template's parameters: changing the connection does not leave the template.
  const setParams = (params: {
    connection?: string | null;
    collection?: number;
    template?: number;
  }) => {
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current);
        if (params.connection !== undefined) {
          if (params.connection === null) {
            next.delete(CONNECTION_PARAM);
          } else {
            next.set(CONNECTION_PARAM, params.connection);
          }
        }
        if (params.collection !== undefined && params.template !== undefined) {
          next.set(COLLECTION_PARAM, String(params.collection));
          next.set(TEMPLATE_PARAM, String(params.template));
        }
        return next;
      },
      { replace: true },
    );
  };

  const chooseConnection = (connectionId: string | null) => {
    form.setFieldValue('connectionId', connectionId);
    setParams({ connection: connectionId });
  };

  // Fills the form once per template opened, as soon as the template and connections are known.
  const loadedTemplateId = useRef<number | undefined>(undefined);
  useEffect(() => {
    if (
      template === undefined ||
      !connections.isSuccess ||
      loadedTemplateId.current === template.id
    ) {
      return;
    }
    loadedTemplateId.current = template.id;
    const current = connections.data.find(({ id }) => String(id) === form.getValues().connectionId);
    const connection =
      current?.brokerType === template.brokerType
        ? current
        : connections.data.find(({ brokerType }) => brokerType === template.brokerType);
    const connectionId = connection === undefined ? null : String(connection.id);
    form.setValues(fromTemplate(template, connectionId));
    setParams({ connection: connectionId });
    // Runs when the template or the connections arrive; the form and URL setters are stable.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [template, connections.isSuccess]);

  /** Validates what a template needs, and returns the broker to save it for. */
  const templateBroker = () => {
    const errors = validateTemplateDraft(form.values, broker);
    form.setErrors(errors);
    return Object.keys(errors).length === 0 ? broker : undefined;
  };

  const saveTemplate = () => {
    const target = templateBroker();
    if (target === undefined || template === undefined || collectionId === undefined) {
      return;
    }
    replaceTemplate.mutate(
      {
        collectionId,
        templateId: template.id,
        request: toTemplateRequest(form.values, template.name, target),
      },
      {
        onSuccess: (saved) => {
          notifications.show({ color: 'green', message: `Template ${saved.name} saved` });
        },
        onError: (error) => {
          showProblem(error, 'The template could not be saved');
        },
      },
    );
  };

  const send = (values: ProduceFormValues) => {
    const broker = brokerOf(values.connectionId);
    if (values.connectionId === null || broker === undefined) {
      return;
    }
    const request = toProduceRequest(values, Number(values.connectionId), broker);
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
      {templateId !== undefined && collection.isSuccess && template === undefined && (
        <Alert color="orange" title="Template not found">
          This collection has no such template any more.
        </Alert>
      )}
      {template !== undefined && collection.data !== undefined && (
        <Alert title={`Editing the template ${template.name}`}>
          <Group justify="space-between">
            <Text size="sm">
              From the collection{' '}
              <Anchor component={Link} to={`/collections/${String(collection.data.id)}`}>
                {collection.data.name}
              </Anchor>
              . Send it from there to resolve its placeholders.
            </Text>
            <Button size="xs" loading={replaceTemplate.isPending} onClick={saveTemplate}>
              Save template
            </Button>
          </Group>
        </Alert>
      )}
      <Grid>
        <Grid.Col span={{ base: 12, md: 7 }}>
          <Paper withBorder p="md">
            <form onSubmit={form.onSubmit(send)} noValidate aria-label="Produce a message">
              <Stack>
                <Select
                  label="Connection"
                  withAsterisk
                  placeholder={connections.isPending ? 'Loading connections…' : 'Choose one'}
                  data={connectionOptions(savedConnections)}
                  {...form.getInputProps('connectionId')}
                  onChange={chooseConnection}
                />
                <DestinationInputs
                  destinationType={destinationType}
                  nameProps={form.getInputProps('destinationName')}
                  routingKeyProps={form.getInputProps('routingKey')}
                />
                {(broker === undefined || broker.supportsMessageKeys) && (
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
                  <Group gap="xs">
                    <Button
                      variant="default"
                      onClick={() => {
                        if (templateBroker() !== undefined) {
                          setSaving(true);
                        }
                      }}
                    >
                      Save as template
                    </Button>
                    <Button type="submit" loading={produce.isPending}>
                      Send
                    </Button>
                  </Group>
                </Group>
                {hasPlaceholders(form.values) && (
                  <Text size="sm" c="dimmed">
                    Placeholders such as {'{{name}}'} are sent as written from here. Save the
                    message as a template and send it from its collection to resolve them.
                  </Text>
                )}
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
      {saving && broker !== undefined && (
        <SaveTemplateModal
          values={form.values}
          broker={broker}
          onClose={() => {
            setSaving(false);
          }}
          onSaved={(savedCollectionId, savedTemplateId) => {
            setSaving(false);
            loadedTemplateId.current = savedTemplateId;
            setParams({ collection: savedCollectionId, template: savedTemplateId });
          }}
        />
      )}
    </Stack>
  );
}
