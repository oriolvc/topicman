import { Alert, Badge, Loader } from '@mantine/core';
import { ApiProblem } from '../../api/problem';
import type { StreamStatus } from './useConsumeStream';

/** Where the screen's stream is: not started, paused by the user, or what the stream reports. */
export type ConsumeState = { kind: 'idle' } | { kind: 'paused' } | StreamStatus;

type ConsumeStatusProps = {
  state: ConsumeState;
  /** Why the last attempt to open a session failed, if it did. */
  openError: unknown;
};

export function ConsumeStatus({ state, openError }: ConsumeStatusProps) {
  if (openError !== null && openError !== undefined) {
    return openError instanceof ApiProblem ? (
      <Alert color="red" title={openError.problem.title ?? 'Could not start consuming'}>
        {openError.message}
      </Alert>
    ) : (
      <Alert color="red" title="Could not start consuming">
        TopicMan could not be reached. Check that it is running.
      </Alert>
    );
  }
  switch (state.kind) {
    case 'idle':
      return null;
    case 'connecting':
      return (
        <Badge role="status" color="gray" leftSection={<Loader size={10} color="gray" />}>
          Connecting
        </Badge>
      );
    case 'streaming':
      return (
        <Badge role="status" color="green">
          Streaming
        </Badge>
      );
    case 'reconnecting':
      return (
        <Badge role="status" color="yellow" leftSection={<Loader size={10} color="yellow" />}>
          Reconnecting
        </Badge>
      );
    case 'paused':
      return (
        <Alert title="Paused">
          Messages wait on the broker until you resume, for as long as the session stays open.
        </Alert>
      );
    case 'replaced':
      return (
        <Alert color="yellow" title="Streaming in another window">
          Another window took this session over. Resume here to take it back.
        </Alert>
      );
    case 'closed':
      return (
        <Alert color="gray" title="The session was closed">
          It was closed elsewhere or stayed idle too long. Start again to keep consuming.
        </Alert>
      );
    case 'lost':
      return (
        <Alert color="red" title="The stream was lost">
          TopicMan could not be reached, or the session no longer exists. Start again to keep
          consuming.
        </Alert>
      );
    case 'failed':
      return (
        <Alert color="red" title={state.problem.title ?? 'Read failed'}>
          {state.problem.detail ?? 'Reading the destination failed.'}
        </Alert>
      );
  }
}
