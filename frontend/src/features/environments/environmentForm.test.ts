import { describe, expect, it } from 'vitest';
import { anEnvironment } from '../../test/environmentFixtures';
import {
  existingEnvironmentValues,
  newEnvironmentValues,
  toEnvironmentRequest,
  validateEnvironment,
  type EnvironmentFormValues,
} from './environmentForm';

function values(
  variables: { name: string; value: string }[],
  name = 'staging',
): EnvironmentFormValues {
  return {
    name,
    variables: variables.map((variable, index) => ({ key: String(index), ...variable })),
  };
}

describe('environmentForm', () => {
  it('starts empty, or from an existing environment', () => {
    expect(newEnvironmentValues()).toEqual({ name: '', variables: [] });
    expect(existingEnvironmentValues(anEnvironment()).variables).toEqual([
      { key: expect.any(String) as string, name: 'env', value: 'stg' },
    ]);
  });

  it('accepts names a placeholder can use, and empty values', () => {
    expect(validateEnvironment(values([{ name: 'kafka.topic-2', value: '' }]))).toEqual({});
  });

  it('asks for a name', () => {
    expect(validateEnvironment(values([], ' '))).toEqual({ name: 'Enter a name' });
  });

  it('rejects variable names a placeholder cannot use', () => {
    const errors = validateEnvironment(
      values([
        { name: '1st', value: 'x' },
        { name: '$uuid', value: 'x' },
      ]),
    );

    expect(errors['variables.0.name']).toBe(
      'Start with a letter or _, then use letters, digits, _, . or -',
    );
    expect(errors['variables.1.name']).toBe('Names starting with $ are built-in variables');
  });

  it('rejects the same variable twice', () => {
    const errors = validateEnvironment(
      values([
        { name: 'env', value: 'a' },
        { name: 'env', value: 'b' },
      ]),
    );

    expect(errors['variables.1.name']).toBe('This variable is already set');
  });

  it('sends the trimmed name and the variables in order', () => {
    expect(
      toEnvironmentRequest(
        values(
          [
            { name: ' env ', value: ' stg ' },
            { name: 'b', value: '' },
          ],
          ' staging ',
        ),
      ),
    ).toEqual({
      name: 'staging',
      variables: [
        { name: 'env', value: ' stg ' },
        { name: 'b', value: '' },
      ],
    });
  });
});
