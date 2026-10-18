import { Switch, useComputedColorScheme, useMantineColorScheme } from '@mantine/core';

export function ColorSchemeToggle() {
  const { setColorScheme } = useMantineColorScheme();
  const colorScheme = useComputedColorScheme('light');

  return (
    <Switch
      label="Dark mode"
      checked={colorScheme === 'dark'}
      onChange={(event) => {
        setColorScheme(event.currentTarget.checked ? 'dark' : 'light');
      }}
    />
  );
}
