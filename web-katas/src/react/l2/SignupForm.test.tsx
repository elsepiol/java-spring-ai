import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { SignupForm } from './SignupForm';

function deferred() {
  let resolve!: () => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('SignupForm', () => {
  it('validates before submitting and links the messages to the fields', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<SignupForm onSubmit={onSubmit} />);
    await user.type(screen.getByLabelText('Email'), 'not-an-email');
    await user.type(screen.getByLabelText('Password'), 'short');
    await user.click(screen.getByRole('button', { name: 'Sign up' }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Enter a valid email');
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('Use at least 12 characters');
  });

  it('submits valid data once and shows progress', async () => {
    const user = userEvent.setup();
    const pending = deferred();
    const onSubmit = vi.fn(() => pending.promise);
    render(<SignupForm onSubmit={onSubmit} />);
    await user.type(screen.getByLabelText('Email'), 'ada@example.com');
    await user.type(screen.getByLabelText('Password'), 'correct horse battery');
    await user.click(screen.getByRole('button', { name: 'Sign up' }));

    expect(onSubmit).toHaveBeenCalledWith({ email: 'ada@example.com', password: 'correct horse battery' });
    const busy = screen.getByRole('button', { name: 'Signing up…' });
    expect(busy).toBeDisabled();
    await user.click(busy);
    expect(onSubmit).toHaveBeenCalledTimes(1);

    pending.resolve();
    expect(await screen.findByRole('status')).toHaveTextContent('Welcome!');
  });

  it('shows server errors and lets the user retry', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValueOnce(new Error('Email already registered')).mockResolvedValueOnce(undefined);
    render(<SignupForm onSubmit={onSubmit} />);
    await user.type(screen.getByLabelText('Email'), 'ada@example.com');
    await user.type(screen.getByLabelText('Password'), 'correct horse battery');
    await user.click(screen.getByRole('button', { name: 'Sign up' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Email already registered');
    await user.click(screen.getByRole('button', { name: 'Sign up' }));
    expect(await screen.findByText('Welcome!')).toBeInTheDocument();
  });
});
