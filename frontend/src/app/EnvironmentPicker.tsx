import { Select } from '@mantine/core';
import { useActiveEnvironment } from '../shared/environments';

/** Chooses the environment templates are resolved against, for the whole app. */
export function EnvironmentPicker() {
  const { environment, choose, environments } = useActiveEnvironment();
  return (
    <Select
      aria-label="Environment"
      placeholder="No environment"
      clearable
      w={200}
      data={(environments.data ?? []).map(({ id, name }) => ({ value: String(id), label: name }))}
      value={environment === undefined ? null : String(environment.id)}
      onChange={(value) => {
        choose(value === null ? null : Number(value));
      }}
    />
  );
}
