import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '../test/renderWithProviders';
import { App } from './App';

describe('App', () => {
  it('shows the application name', () => {
    renderWithProviders(<App />);

    expect(screen.getByRole('heading', { name: 'TopicMan' })).toBeDefined();
  });

  it('switches to dark mode', () => {
    renderWithProviders(<App />);
    const toggle = screen.getByRole('switch', { name: 'Dark mode' });

    fireEvent.click(toggle);

    expect(screen.getByRole<HTMLInputElement>('switch', { name: 'Dark mode' }).checked).toBe(true);
  });
});
