import { Button } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import type { ConnectionResponse } from '../../api/connection';
import { showProblem } from '../../shared/showProblem';
import { useTestConnection } from './connectionQueries';

type TestConnectionButtonProps = {
  connection: ConnectionResponse;
};

/** Tests one connection and tells what answered, or why the broker could not be reached. */
export function TestConnectionButton({ connection }: TestConnectionButtonProps) {
  const test = useTestConnection();
  return (
    <Button
      size="xs"
      variant="default"
      aria-label={`Test ${connection.name}`}
      loading={test.isPending}
      onClick={() => {
        test.mutate(connection.id, {
          onSuccess: ({ status, detail, durationMs }) => {
            notifications.show(
              status === 'SUCCEEDED'
                ? {
                    color: 'green',
                    title: `${connection.name} works`,
                    message: `${detail} (${String(durationMs)} ms)`,
                  }
                : { color: 'red', title: `${connection.name} failed the test`, message: detail },
            );
          },
          onError: (error) => {
            showProblem(error, 'The connection could not be tested');
          },
        });
      }}
    >
      Test
    </Button>
  );
}
