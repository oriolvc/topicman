import { randomId } from '@mantine/hooks';
import type { EnvironmentRequest, EnvironmentResponse } from '../../api/collection';

export type VariableRow = {
  /** Stable React key for the row; never sent. */
  key: string;
  name: string;
  value: string;
};

export type EnvironmentFormValues = {
  name: string;
  variables: VariableRow[];
};

/** As written in a `{{name}}` placeholder (ADR 0006); the API checks it too. */
const VARIABLE_NAME = /^[A-Za-z_][A-Za-z0-9_.-]*$/;

export function newEnvironmentValues(): EnvironmentFormValues {
  return { name: '', variables: [] };
}

export function existingEnvironmentValues(environment: EnvironmentResponse): EnvironmentFormValues {
  return {
    name: environment.name,
    variables: environment.variables.map((variable) => ({ key: randomId(), ...variable })),
  };
}

/** Errors keyed by form path (`variables.0.name`), as `@mantine/form` expects. */
export function validateEnvironment(values: EnvironmentFormValues): Record<string, string> {
  const errors: Record<string, string> = {};
  if (values.name.trim() === '') {
    errors['name'] = 'Enter a name';
  }
  const seen = new Set<string>();
  values.variables.forEach((variable, index) => {
    const path = `variables.${String(index)}.name`;
    const name = variable.name.trim();
    if (name.startsWith('$')) {
      errors[path] = 'Names starting with $ are built-in variables';
    } else if (!VARIABLE_NAME.test(name)) {
      errors[path] = 'Start with a letter or _, then use letters, digits, _, . or -';
    } else if (seen.has(name)) {
      errors[path] = 'This variable is already set';
    }
    seen.add(name);
  });
  return errors;
}

/** Values are sent as typed: spaces may be part of what a template needs. */
export function toEnvironmentRequest(values: EnvironmentFormValues): EnvironmentRequest {
  return {
    name: values.name.trim(),
    variables: values.variables.map(({ name, value }) => ({ name: name.trim(), value })),
  };
}
