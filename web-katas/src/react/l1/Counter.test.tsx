import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Counter } from './Counter';

const value = () => screen.getByRole('status');
const button = (name: string) => screen.getByRole('button', { name });

describe('Counter', () => {
  it('starts at the initial value', () => {
    render(<Counter initial={3} />);
    expect(value()).toHaveTextContent('3');
  });

  it('defaults to 1', () => {
    render(<Counter />);
    expect(value()).toHaveTextContent('1');
  });

  it('increases and decreases', async () => {
    const user = userEvent.setup();
    render(<Counter initial={2} />);
    await user.click(button('Increase'));
    await user.click(button('Increase'));
    expect(value()).toHaveTextContent('4');
    await user.click(button('Decrease'));
    expect(value()).toHaveTextContent('3');
  });

  it('disables the buttons at the limits', async () => {
    const user = userEvent.setup();
    render(<Counter initial={1} min={1} max={2} />);
    expect(button('Decrease')).toBeDisabled();
    await user.click(button('Increase'));
    expect(button('Increase')).toBeDisabled();
    expect(button('Decrease')).toBeEnabled();
    expect(value()).toHaveTextContent('2');
  });

  it('resets to the initial value', async () => {
    const user = userEvent.setup();
    render(<Counter initial={5} />);
    await user.click(button('Increase'));
    await user.click(button('Reset'));
    expect(value()).toHaveTextContent('5');
  });

  it('reports changes but not the first render', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Counter initial={1} onChange={onChange} />);
    expect(onChange).not.toHaveBeenCalled();
    await user.click(button('Increase'));
    expect(onChange).toHaveBeenLastCalledWith(2);
  });
});
