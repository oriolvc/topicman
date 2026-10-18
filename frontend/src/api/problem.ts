import type { components } from './generated/connection';

/** An RFC 9457 problem, as every API error is returned (`code` is the stable domain error code). */
export type ProblemDetail = components['schemas']['ProblemDetail'];

/** A failed API call, carrying the problem the server returned or one describing the failure. */
export class ApiProblem extends Error {
  readonly status: number;
  readonly problem: ProblemDetail;

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${String(status)}`);
    this.name = 'ApiProblem';
    this.status = status;
    this.problem = problem;
  }

  get code(): string | undefined {
    return this.problem.code;
  }
}

type FetchResult<T> = {
  data?: T;
  error?: unknown;
  response: Response;
};

/**
 * Returns the data of a successful `openapi-fetch` call, or throws an `ApiProblem`, so TanStack
 * Query sees failed calls as errors.
 */
export function unwrap<T>(result: FetchResult<T>): T {
  const { data, error, response } = result;
  if (!response.ok || error !== undefined) {
    throw new ApiProblem(response.status, toProblem(error, response));
  }
  // A 204 has no body: its data is undefined, and so is its type.
  return data as T;
}

function toProblem(error: unknown, response: Response): ProblemDetail {
  if (isProblemDetail(error)) {
    return error;
  }
  // Not from the API itself, such as a proxy's error page while the backend is down.
  return {
    title: response.statusText || 'Request failed',
    status: response.status,
    detail: `The server answered ${String(response.status)} without a problem description.`,
  };
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const { title, status } = value as Record<string, unknown>;
  return typeof title === 'string' || typeof status === 'number';
}
