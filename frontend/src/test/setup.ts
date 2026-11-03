import { cleanup } from '@testing-library/react';
import { EventSource as FetchEventSource } from 'eventsource';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { server } from './server';

beforeAll(() => {
  server.listen({ onUnhandledFrame: 'error' });
});

afterEach(() => {
  cleanup();
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});

// jsdom implements none of these, and Mantine needs them to resolve the color scheme and layout.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string): MediaQueryList => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false,
  }),
});

class NoopResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = NoopResizeObserver;

Element.prototype.scrollIntoView = () => undefined;

// Mantine's autosizing textareas resize once fonts load.
Object.defineProperty(document, 'fonts', {
  value: {
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  },
});

// jsdom has no EventSource. This one reads the stream with fetch, so MSW's `sse()` handlers serve it.
globalThis.EventSource = FetchEventSource;

// jsdom has no object URLs; downloads only need one to exist.
URL.createObjectURL = () => 'blob:topicman-test';
URL.revokeObjectURL = () => undefined;
