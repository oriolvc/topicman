import { describe, expect, it } from 'vitest';
import { ApiProblem, unwrap } from './problem';

function response(status: number, statusText = ''): Response {
  return new Response(null, { status, statusText });
}

function captureProblem(action: () => unknown): ApiProblem {
  try {
    action();
  } catch (error) {
    if (error instanceof ApiProblem) {
      return error;
    }
    throw error;
  }
  throw new Error('Expected an ApiProblem to be thrown');
}

describe('unwrap', () => {
  it('returns the data of a successful call', () => {
    const data = { receipts: ['orders-0@42'] };

    const result = unwrap({ data, response: response(200) });

    expect(result).toEqual(data);
  });

  it('returns nothing for a call without content', () => {
    const result = unwrap<string | undefined>({ response: response(204) });

    expect(result).toBeUndefined();
  });

  it('throws the problem the server returned', () => {
    const problem = {
      title: 'Connection name taken',
      status: 409,
      detail: "Another connection is already named 'local'",
      code: 'connection.name-taken',
    };

    const thrown = captureProblem(() => unwrap({ error: problem, response: response(409) }));

    expect(thrown.status).toBe(409);
    expect(thrown.code).toBe('connection.name-taken');
    expect(thrown.problem).toEqual(problem);
    expect(thrown.message).toBe("Another connection is already named 'local'");
  });

  it('keeps extension properties of the problem', () => {
    const problem = {
      title: 'Produce failed',
      status: 503,
      code: 'messaging.produce-failed',
      sent: 2,
    };

    const thrown = captureProblem(() => unwrap({ error: problem, response: response(503) }));

    expect(thrown.problem['sent']).toBe(2);
  });

  it('describes a failure that is not a problem', () => {
    const thrown = captureProblem(() =>
      unwrap({ error: '<html>Bad Gateway</html>', response: response(502, 'Bad Gateway') }),
    );

    expect(thrown.status).toBe(502);
    expect(thrown.code).toBeUndefined();
    expect(thrown.problem.title).toBe('Bad Gateway');
  });
});
