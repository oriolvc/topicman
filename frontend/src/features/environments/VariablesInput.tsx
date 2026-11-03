import { Button, CloseButton, Group, Input, Stack, TextInput } from '@mantine/core';
import type { UseFormReturnType } from '@mantine/form';
import { randomId } from '@mantine/hooks';
import type { EnvironmentFormValues } from './environmentForm';

type VariablesInputProps = {
  form: UseFormReturnType<EnvironmentFormValues>;
};

export function VariablesInput({ form }: VariablesInputProps) {
  const { variables } = form.values;

  return (
    <Input.Wrapper label="Variables" description="Used as {{name}} in templates">
      <Stack gap="xs" mt={4}>
        {variables.map((variable, index) => {
          const position = String(index + 1);
          return (
            <Group key={variable.key} gap="xs" align="flex-start" wrap="nowrap">
              <TextInput
                flex={1}
                placeholder="Name"
                aria-label={`Variable ${position} name`}
                {...form.getInputProps(`variables.${String(index)}.name`)}
              />
              <TextInput
                flex={1}
                placeholder="Value"
                aria-label={`Variable ${position} value`}
                {...form.getInputProps(`variables.${String(index)}.value`)}
              />
              <CloseButton
                mt={4}
                aria-label={`Remove variable ${position}`}
                onClick={() => {
                  form.removeListItem('variables', index);
                }}
              />
            </Group>
          );
        })}
        <Group>
          <Button
            variant="default"
            size="xs"
            onClick={() => {
              form.insertListItem('variables', { key: randomId(), name: '', value: '' });
            }}
          >
            Add variable
          </Button>
        </Group>
      </Stack>
    </Input.Wrapper>
  );
}
