import { Button, CloseButton, Group, Input, Stack, TextInput } from '@mantine/core';
import type { UseFormReturnType } from '@mantine/form';
import { randomId } from '@mantine/hooks';
import type { ProduceFormValues } from './produceForm';

type HeadersInputProps = {
  form: UseFormReturnType<ProduceFormValues>;
};

export function HeadersInput({ form }: HeadersInputProps) {
  const { headers } = form.values;

  return (
    <Input.Wrapper label="Headers">
      <Stack gap="xs" mt={4}>
        {headers.map((header, index) => {
          const position = String(index + 1);
          return (
            <Group key={header.key} gap="xs" align="flex-start" wrap="nowrap">
              <TextInput
                flex={1}
                placeholder="Name"
                aria-label={`Header ${position} name`}
                {...form.getInputProps(`headers.${String(index)}.name`)}
              />
              <TextInput
                flex={1}
                placeholder="Value"
                aria-label={`Header ${position} value`}
                {...form.getInputProps(`headers.${String(index)}.value`)}
              />
              <CloseButton
                mt={4}
                aria-label={`Remove header ${position}`}
                onClick={() => {
                  form.removeListItem('headers', index);
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
              form.insertListItem('headers', { key: randomId(), name: '', value: '' });
            }}
          >
            Add header
          </Button>
        </Group>
      </Stack>
    </Input.Wrapper>
  );
}
