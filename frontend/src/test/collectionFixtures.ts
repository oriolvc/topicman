import type {
  CollectionResponse,
  CollectionSummaryResponse,
  TemplateResponse,
} from '../api/collection';

export function aTemplate(overrides: Partial<TemplateResponse> = {}): TemplateResponse {
  return {
    id: 10,
    name: 'Order created',
    brokerType: 'KAFKA',
    destination: { type: 'TOPIC', name: 'orders-{{env}}' },
    key: '{{orderId}}',
    headers: { 'trace-id': '{{$uuid}}' },
    payload: { format: 'JSON', content: '{"id": "{{orderId}}"}' },
    ...overrides,
  };
}

export function aCollection(overrides: Partial<CollectionResponse> = {}): CollectionResponse {
  return {
    id: 1,
    name: 'Orders',
    templates: [aTemplate()],
    createdAt: '2026-11-03T10:00:00Z',
    updatedAt: '2026-11-03T10:00:00Z',
    ...overrides,
  };
}

export function summaryOf(collection: CollectionResponse): CollectionSummaryResponse {
  return {
    id: collection.id,
    name: collection.name,
    templateCount: collection.templates.length,
    createdAt: collection.createdAt,
    updatedAt: collection.updatedAt,
  };
}
