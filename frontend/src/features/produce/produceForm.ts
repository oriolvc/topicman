import type { Broker, Payload, ProduceMessageRequest } from '../../api/messaging';
import { destinationError, toDestination, type DestinationValues } from '../../shared/destinations';

export const MAX_COPIES = 100;

export type HeaderRow = {
  /** Stable React key for the row; never sent. */
  key: string;
  name: string;
  value: string;
};

export type ProduceFormValues = DestinationValues & {
  connectionId: string | null;
  messageKey: string;
  headers: HeaderRow[];
  payloadFormat: Payload['format'];
  payload: string;
  /** A string while the number input is being edited. */
  copies: number | string;
};

export function initialProduceValues(connectionId: string | null): ProduceFormValues {
  return {
    connectionId,
    destinationName: '',
    routingKey: '',
    messageKey: '',
    headers: [],
    payloadFormat: 'JSON',
    payload: '',
    copies: 1,
  };
}

/**
 * Errors keyed by form path (`headers.0.name`), as `@mantine/form` expects. `broker` is the
 * selected connection's broker, or `undefined` when no saved connection is selected.
 */
export function validateProduce(
  values: ProduceFormValues,
  broker: Broker | undefined,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (broker === undefined) {
    errors['connectionId'] = 'Choose a connection';
  } else {
    const destination = destinationError(values, broker);
    if (destination !== undefined) {
      errors['destinationName'] = destination;
    }
  }
  const seen = new Set<string>();
  values.headers.forEach((header, index) => {
    const name = header.name.trim();
    if (name === '') {
      errors[`headers.${String(index)}.name`] = 'Enter a header name';
    } else if (seen.has(name)) {
      errors[`headers.${String(index)}.name`] = 'This header is already set';
    }
    seen.add(name);
  });
  if (values.payloadFormat === 'JSON') {
    const jsonError = jsonProblem(values.payload);
    if (jsonError !== null) {
      errors['payload'] = jsonError;
    }
  }
  if (!isValidCopies(values.copies)) {
    errors['copies'] = `Send between 1 and ${String(MAX_COPIES)} copies`;
  }
  return errors;
}

function jsonProblem(payload: string): string | null {
  if (payload.trim() === '') {
    return 'Enter a JSON payload';
  }
  try {
    JSON.parse(payload);
    return null;
  } catch {
    return 'The payload is not valid JSON';
  }
}

function isValidCopies(copies: number | string): copies is number {
  return (
    typeof copies === 'number' && Number.isInteger(copies) && copies >= 1 && copies <= MAX_COPIES
  );
}

export function toProduceRequest(
  values: ProduceFormValues,
  connectionId: number,
  broker: Broker,
): ProduceMessageRequest {
  const messageKey = values.messageKey;
  const headers = Object.fromEntries(
    values.headers.map(({ name, value }) => [name.trim(), value] as const),
  );
  return {
    connectionId,
    destination: toDestination(values, broker),
    ...(broker.supportsMessageKeys && messageKey !== '' && { key: messageKey }),
    ...(values.headers.length > 0 && { headers }),
    payload: { format: values.payloadFormat, content: values.payload },
    copies: typeof values.copies === 'number' ? values.copies : 1,
  };
}
