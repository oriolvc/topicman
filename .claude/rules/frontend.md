---
paths:
  - "frontend/**/*.{ts,tsx}"
---

# Frontend conventions

## Structure

```
frontend/src/
├── app/         app shell, routing, providers (MantineProvider, QueryClientProvider)
├── features/    one folder per feature: connections/, produce/, consume/, collections/
│   └── <feature>/  components, hooks, api calls and types for that feature only
├── shared/      reusable components, hooks and utilities used by 2+ features
└── api/         generated API types and the typed HTTP client
```

- Features never import from another feature; shared code moves to `shared/`.
- One component per file, PascalCase file name matching the component: `ProduceForm.tsx`.

## TypeScript

- `strict` mode on; no `any`, no non-null assertions (`!`). Use `unknown` and narrow.
- API types are generated from the backend OpenAPI spec into `src/api/`; never hand-write them.
- Prefer type aliases and discriminated unions; no enums (use string literal unions).

## React

- Function components and hooks only.
- Server state lives in TanStack Query (one query key factory per feature). No Redux, no global
  store; local UI state with `useState`.
- Live consume streams use `EventSource` (SSE) wrapped in a dedicated hook.
- Forms use `@mantine/form` with validation next to the form.

## UI

- Mantine components only; no other UI kits and no custom CSS frameworks.
- Theme tokens (colors, spacing, radius) come from the Mantine theme, never hard-coded values.
- Support light and dark mode through the Mantine color scheme.
- User feedback through `@mantine/notifications`; errors show the `ProblemDetail` title and detail.

## Style

- ESLint + Prettier own formatting; run `npm run lint -- --fix` instead of hand-formatting.
- Named exports only, except where a framework requires a default export.
