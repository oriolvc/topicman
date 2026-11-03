import { Alert, Anchor, Button, Group, Modal, NumberInput, Select, Stack } from '@mantine/core';
import { useState } from 'react';
import { Link } from 'react-router';
import type { TemplateResponse } from '../../api/collection';
import { BROKER_LABELS } from '../../shared/brokers';
import { connectionOptions, useConnections } from '../../shared/connections';
import { useActiveEnvironment } from '../../shared/environments';
import { useProduceMessage } from '../../shared/produceMessage';
import { ProduceResult, type ProduceOutcome } from '../../shared/ProduceResult';
import { useResolveTemplate } from './collectionQueries';
import { ResolutionPreview, type Resolution } from './ResolutionPreview';

const MAX_COPIES = 100;

type SendTemplateModalProps = {
  collectionId: number;
  template: TemplateResponse;
  onClose: () => void;
};

/**
 * Resolves a template against an environment and produces it through a connection of its broker
 * type (ADR 0006). Every send resolves again, so built-in variables such as {{$uuid}} are fresh.
 */
export function SendTemplateModal({ collectionId, template, onClose }: SendTemplateModalProps) {
  const connections = useConnections();
  const { environment: activeEnvironment, environments } = useActiveEnvironment();
  const resolve = useResolveTemplate();
  const produce = useProduceMessage();
  const [chosenConnection, setChosenConnection] = useState<string | null | undefined>(undefined);
  // Undefined until changed here: the dialog follows the environment chosen in the header.
  const [chosenEnvironment, setChosenEnvironment] = useState<string | null | undefined>(undefined);
  const [copies, setCopies] = useState<number | string>(1);
  const [resolution, setResolution] = useState<Resolution | undefined>(undefined);
  const [outcome, setOutcome] = useState<ProduceOutcome | undefined>(undefined);

  const matching = (connections.data ?? []).filter(
    ({ brokerType }) => brokerType === template.brokerType,
  );
  const firstMatching = matching[0];
  const connectionId =
    chosenConnection === undefined
      ? firstMatching === undefined
        ? null
        : String(firstMatching.id)
      : chosenConnection;
  const environmentId =
    chosenEnvironment === undefined
      ? activeEnvironment === undefined
        ? null
        : String(activeEnvironment.id)
      : chosenEnvironment;
  const environmentName = environments.data?.find(({ id }) => String(id) === environmentId)?.name;
  const validCopies =
    typeof copies === 'number' && Number.isInteger(copies) && copies >= 1 && copies <= MAX_COPIES;

  const resolveNow = () =>
    resolve.mutateAsync({
      collectionId,
      templateId: template.id,
      environmentId: environmentId === null ? undefined : Number(environmentId),
    });

  const preview = async () => {
    setOutcome(undefined);
    try {
      setResolution({ kind: 'resolved', message: await resolveNow() });
    } catch (error) {
      setResolution({ kind: 'failed', error });
    }
  };

  const send = async () => {
    if (connectionId === null || !validCopies) {
      return;
    }
    setOutcome(undefined);
    let message;
    try {
      message = await resolveNow();
      setResolution({ kind: 'resolved', message });
    } catch (error) {
      setResolution({ kind: 'failed', error });
      return;
    }
    try {
      const { receipts } = await produce.mutateAsync({
        connectionId: Number(connectionId),
        destination: message.destination,
        ...(message.key !== undefined && { key: message.key }),
        headers: message.headers,
        payload: message.payload,
        copies,
      });
      setOutcome({ kind: 'sent', receipts });
    } catch (error) {
      setOutcome({ kind: 'failed', error, copies });
    }
  };

  const brokerLabel = BROKER_LABELS[template.brokerType];

  return (
    <Modal opened onClose={onClose} title={`Send ${template.name}`} size="lg">
      <Stack>
        {connections.isSuccess && matching.length === 0 ? (
          <Alert title={`No ${brokerLabel} connection yet`}>
            This template is for {brokerLabel}.{' '}
            <Anchor component={Link} to="/connections">
              Create a connection
            </Anchor>{' '}
            to send it.
          </Alert>
        ) : (
          <Select
            label="Connection"
            withAsterisk
            placeholder={connections.isPending ? 'Loading connections…' : 'Choose one'}
            data={connectionOptions(matching)}
            value={connectionId}
            onChange={setChosenConnection}
          />
        )}
        <Group grow align="flex-start">
          <Select
            label="Environment"
            placeholder="No environment"
            clearable
            data={(environments.data ?? []).map(({ id, name }) => ({
              value: String(id),
              label: name,
            }))}
            value={environmentId}
            onChange={(value) => {
              setChosenEnvironment(value);
              setResolution(undefined);
            }}
          />
          <NumberInput
            label="Copies"
            min={1}
            max={MAX_COPIES}
            allowDecimal={false}
            allowNegative={false}
            value={copies}
            onChange={setCopies}
            error={validCopies ? undefined : `Send between 1 and ${String(MAX_COPIES)} copies`}
          />
        </Group>
        {resolution !== undefined && (
          <ResolutionPreview resolution={resolution} environmentName={environmentName} />
        )}
        {outcome !== undefined && <ProduceResult outcome={outcome} />}
        <Group justify="flex-end">
          <Button
            variant="default"
            loading={resolve.isPending && !produce.isPending}
            onClick={() => void preview()}
          >
            Preview
          </Button>
          <Button
            disabled={connectionId === null || !validCopies}
            loading={produce.isPending}
            onClick={() => void send()}
          >
            Send
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
