import {
  Alert,
  Anchor,
  Button,
  Group,
  Input,
  List,
  Paper,
  SegmentedControl,
  Select,
  Stack,
  Text,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import type { ConsumeSessionResponse, StartPosition } from '../../api/messaging';
import { BROKER_LABELS, useBrokers } from '../../shared/brokers';
import { connectionOptions, useConnections } from '../../shared/connections';
import { DestinationInputs } from '../../shared/DestinationInputs';
import { ConsumedMessageCard } from './ConsumedMessageCard';
import { addMessage, MAX_MESSAGES, type ConsumedMessage } from './consumedMessages';
import {
  initialConsumeValues,
  toOpenRequest,
  validateConsume,
  type ConsumeFormValues,
} from './consumeForm';
import { closeConsumeSession, useOpenConsumeSession } from './consumeQueries';
import { ConsumeStatus, type ConsumeState } from './ConsumeStatus';
import { useConsumeStream } from './useConsumeStream';

/** The selected connection is kept in the URL (`?connection=<id>`), so it can be bookmarked. */
const CONNECTION_PARAM = 'connection';

/** The open session; pausing disconnects from it, and the server pauses reading meanwhile. */
type Run = { session: ConsumeSessionResponse; paused: boolean; attempt: number };

/** The stream will not resume on its own, so the session is over for this screen. */
function isEnded(state: ConsumeState): boolean {
  return ['closed', 'replaced', 'lost', 'failed'].includes(state.kind);
}

export function ConsumePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const connections = useConnections();
  const brokers = useBrokers();
  const open = useOpenConsumeSession();
  const [run, setRun] = useState<Run | undefined>(undefined);
  const [messages, setMessages] = useState<ConsumedMessage[]>([]);
  const [raw, setRaw] = useState(false);
  const nextSeq = useRef(0);
  const form = useForm<ConsumeFormValues>({
    mode: 'controlled',
    initialValues: initialConsumeValues(searchParams.get(CONNECTION_PARAM)),
    validate: (values) => validateConsume(values, brokerOf(values.connectionId)),
  });

  const status = useConsumeStream(
    run === undefined || run.paused
      ? undefined
      : { eventsUrl: run.session.eventsUrl, attempt: run.attempt },
    (event) => {
      const message = { ...event, seq: nextSeq.current++ };
      setMessages((current) => addMessage(current, message));
    },
  );

  // Every session this screen drops (stopped, replaced by a new one, or left behind) is closed.
  const sessionId = run?.session.id;
  useEffect(() => {
    if (sessionId === undefined) {
      return;
    }
    return () => {
      closeConsumeSession(sessionId);
    };
  }, [sessionId]);

  const savedConnections = connections.data ?? [];
  const brokerOf = (connectionId: string | null) => {
    const brokerType = savedConnections.find(({ id }) => id === connectionId)?.brokerType;
    return brokers.data?.find(({ type }) => type === brokerType);
  };
  const broker = brokerOf(form.values.connectionId);
  const destinationType = broker?.destinationType ?? 'TOPIC';
  const earliestSupported = broker === undefined || broker.canReadFromBeginning;

  const state: ConsumeState = run === undefined ? { kind: 'idle' } : (status ?? { kind: 'paused' });
  const active = run !== undefined && !isEnded(state);

  const chooseConnection = (connectionId: string | null) => {
    form.setFieldValue('connectionId', connectionId);
    setSearchParams(connectionId === null ? {} : { [CONNECTION_PARAM]: connectionId }, {
      replace: true,
    });
  };

  const start = (values: ConsumeFormValues) => {
    const broker = brokerOf(values.connectionId);
    if (values.connectionId === null || broker === undefined) {
      return;
    }
    open.mutate(toOpenRequest(values, values.connectionId, broker), {
      onSuccess: (session) => {
        setMessages([]);
        setRun({ session, paused: false, attempt: 0 });
      },
    });
  };

  const stop = () => {
    setRun(undefined);
  };

  const pause = () => {
    if (run !== undefined) {
      setRun({ ...run, paused: true });
    }
  };

  // A new attempt, so the stream reconnects even after it ended, as when another window took over.
  const resume = () => {
    if (run !== undefined) {
      setRun({ ...run, paused: false, attempt: run.attempt + 1 });
    }
  };

  if (connections.isSuccess && savedConnections.length === 0) {
    return (
      <Stack>
        <Title order={2}>Consume</Title>
        <Alert title="No connections yet">
          <Anchor component={Link} to="/connections">
            Create a connection
          </Anchor>{' '}
          to consume messages.
        </Alert>
      </Stack>
    );
  }

  return (
    <Stack>
      <Title order={2}>Consume</Title>
      <Paper withBorder p="md">
        <form onSubmit={form.onSubmit(start)} noValidate aria-label="Consume a destination">
          <Stack>
            <Select
              label="Connection"
              withAsterisk
              disabled={active}
              placeholder={connections.isPending ? 'Loading connections…' : 'Choose one'}
              data={connectionOptions(savedConnections)}
              {...form.getInputProps('connectionId')}
              onChange={chooseConnection}
            />
            <DestinationInputs
              destinationType={destinationType}
              nameProps={form.getInputProps('destinationName')}
              routingKeyProps={form.getInputProps('routingKey')}
              disabled={active}
            />
            <Group justify="space-between" align="flex-end">
              <Input.Wrapper
                label="Start from"
                description={
                  broker === undefined || broker.canReadFromBeginning
                    ? undefined
                    : `${BROKER_LABELS[broker.type]} only reads messages published from now`
                }
              >
                <SegmentedControl<StartPosition>
                  disabled={active}
                  value={earliestSupported ? form.values.startPosition : 'LATEST'}
                  onChange={(position) => {
                    form.setFieldValue('startPosition', position);
                  }}
                  data={[
                    { value: 'LATEST', label: 'New messages' },
                    { value: 'EARLIEST', label: 'Earliest', disabled: !earliestSupported },
                  ]}
                />
              </Input.Wrapper>
              <Group>
                {state.kind === 'replaced' && (
                  <Button variant="default" onClick={resume}>
                    Resume here
                  </Button>
                )}
                {active ? (
                  <>
                    <Button variant="default" onClick={run.paused ? resume : pause}>
                      {run.paused ? 'Resume' : 'Pause'}
                    </Button>
                    <Button color="red" onClick={stop}>
                      Stop
                    </Button>
                  </>
                ) : (
                  <Button type="submit" loading={open.isPending}>
                    Start
                  </Button>
                )}
              </Group>
            </Group>
          </Stack>
        </form>
      </Paper>
      <ConsumeStatus state={state} openError={open.error} />
      <section aria-label="Messages">
        <Stack>
          <Group justify="space-between">
            <Text size="sm" c="dimmed">
              {messages.length === 1 ? '1 message' : `${String(messages.length)} messages`}
              {messages.length === MAX_MESSAGES && ', older ones are dropped'}
            </Text>
            <Group>
              <SegmentedControl
                size="xs"
                aria-label="Payload display"
                value={raw ? 'raw' : 'pretty'}
                onChange={(display) => {
                  setRaw(display === 'raw');
                }}
                data={[
                  { value: 'pretty', label: 'Pretty' },
                  { value: 'raw', label: 'Raw' },
                ]}
              />
              <Button
                size="xs"
                variant="default"
                disabled={messages.length === 0}
                onClick={() => {
                  setMessages([]);
                }}
              >
                Clear
              </Button>
            </Group>
          </Group>
          <List listStyleType="none" spacing="sm" styles={{ itemWrapper: { width: '100%' } }}>
            {messages.map((message) => (
              <List.Item key={message.seq}>
                <ConsumedMessageCard message={message} raw={raw} />
              </List.Item>
            ))}
          </List>
        </Stack>
      </section>
    </Stack>
  );
}
