import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ErrorBoundary } from './ErrorBoundary';

let explode = true;

function Fragile({ label = 'content' }: { label?: string }) {
  if (explode) throw new Error(`cannot render ${label}`);
  return <p>{label}</p>;
}

const fallback = (error: Error, reset: () => void) => (
  <div role="alert">
    {error.message}
    <button onClick={reset}>Try again</button>
  </div>
);

beforeEach(() => {
  explode = true;
  vi.spyOn(console, 'error').mockImplementation(() => {}); // React logs caught render errors
});
afterEach(() => vi.restoreAllMocks());

describe('ErrorBoundary', () => {
  it('renders children when nothing throws', () => {
    explode = false;
    render(<ErrorBoundary fallback={fallback}><Fragile /></ErrorBoundary>);
    expect(screen.getByText('content')).toBeInTheDocument();
  });

  it('shows the fallback instead of a blank page, and keeps the rest of the app', () => {
    render(
      <main>
        <h1>Shop</h1>
        <ErrorBoundary fallback={fallback}><Fragile /></ErrorBoundary>
      </main>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('cannot render content');
    expect(screen.getByRole('heading', { name: 'Shop' })).toBeInTheDocument();
  });

  it('reports the error', () => {
    const onError = vi.fn();
    render(<ErrorBoundary fallback={fallback} onError={onError}><Fragile /></ErrorBoundary>);
    expect(onError).toHaveBeenCalledWith(expect.objectContaining({ message: 'cannot render content' }));
  });

  it('can be reset by the user', async () => {
    const user = userEvent.setup();
    render(<ErrorBoundary fallback={fallback}><Fragile /></ErrorBoundary>);
    explode = false;
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(screen.getByText('content')).toBeInTheDocument();
  });

  it('resets when a reset key changes', () => {
    const { rerender } = render(<ErrorBoundary fallback={fallback} resetKeys={['/orders/1']}><Fragile label="order 1" /></ErrorBoundary>);
    expect(screen.getByRole('alert')).toBeInTheDocument();
    explode = false;
    rerender(<ErrorBoundary fallback={fallback} resetKeys={['/orders/2']}><Fragile label="order 2" /></ErrorBoundary>);
    expect(screen.getByText('order 2')).toBeInTheDocument();
  });
});
