import { describe, expect, it } from 'vitest';
import { addMessage, formatPayload, MAX_MESSAGES, type ConsumedMessage } from './consumedMessages';

function aMessage(seq: number): ConsumedMessage {
  return {
    seq,
    headers: {},
    payload: { format: 'TEXT', content: `message ${String(seq)}` },
    timestamp: '2026-10-19T10:00:00Z',
    position: `orders-0@${String(seq)}`,
  };
}

describe('addMessage', () => {
  it('puts the newest message first', () => {
    const messages = addMessage(addMessage([], aMessage(0)), aMessage(1));

    expect(messages.map(({ seq }) => seq)).toEqual([1, 0]);
  });

  it(`keeps only the latest ${String(MAX_MESSAGES)} messages`, () => {
    let messages: ConsumedMessage[] = [];
    for (let seq = 0; seq <= MAX_MESSAGES; seq++) {
      messages = addMessage(messages, aMessage(seq));
    }

    expect(messages).toHaveLength(MAX_MESSAGES);
    expect(messages[0]?.seq).toBe(MAX_MESSAGES);
    expect(messages.at(-1)?.seq).toBe(1);
  });
});

describe('formatPayload', () => {
  it('pretty-prints JSON', () => {
    expect(formatPayload({ format: 'JSON', content: '{"id":1,"tags":["a"]}' }, false)).toBe(
      '{\n  "id": 1,\n  "tags": [\n    "a"\n  ]\n}',
    );
  });

  it('shows JSON as received when raw', () => {
    expect(formatPayload({ format: 'JSON', content: '{"id":1}' }, true)).toBe('{"id":1}');
  });

  it('shows text as received', () => {
    expect(formatPayload({ format: 'TEXT', content: '  {not json' }, false)).toBe('  {not json');
  });
});
