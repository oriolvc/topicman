import { Group, TextInput } from '@mantine/core';
import type { GetInputPropsReturnType } from '@mantine/form';
import type { Destination } from '../api/messaging';

type DestinationInputsProps = {
  destinationType: Destination['type'];
  nameProps: GetInputPropsReturnType;
  routingKeyProps: GetInputPropsReturnType;
  disabled?: boolean;
};

/** A topic, or an exchange and routing key, depending on the kind of destination the broker uses. */
export function DestinationInputs({
  destinationType,
  nameProps,
  routingKeyProps,
  disabled = false,
}: DestinationInputsProps) {
  if (destinationType === 'TOPIC') {
    return <TextInput label="Topic" withAsterisk disabled={disabled} {...nameProps} />;
  }
  return (
    <Group grow align="flex-start">
      <TextInput label="Exchange" withAsterisk disabled={disabled} {...nameProps} />
      <TextInput
        label="Routing key"
        description="Empty for fanout exchanges"
        disabled={disabled}
        {...routingKeyProps}
      />
    </Group>
  );
}
