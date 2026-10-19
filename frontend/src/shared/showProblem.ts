import { notifications } from '@mantine/notifications';
import { ApiProblem } from '../api/problem';

/** Tells the user why an action failed, with the problem's title and detail when the API sent one. */
export function showProblem(error: unknown, fallbackTitle: string): void {
  const title =
    error instanceof ApiProblem ? (error.problem.title ?? fallbackTitle) : fallbackTitle;
  const message =
    error instanceof ApiProblem
      ? (error.problem.detail ?? error.message)
      : 'TopicMan could not be reached. Check that it is running.';
  notifications.show({ color: 'red', title, message });
}
