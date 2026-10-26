import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { TodoList } from './TodoList';

async function add(user: ReturnType<typeof userEvent.setup>, text: string) {
  await user.type(screen.getByLabelText('New todo'), text);
  await user.click(screen.getByRole('button', { name: 'Add' }));
}

describe('TodoList', () => {
  it('starts empty', () => {
    render(<TodoList />);
    expect(screen.getByText('Nothing to do')).toBeInTheDocument();
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
    expect(screen.getByText('0 open')).toBeInTheDocument();
  });

  it('adds trimmed items and clears the input', async () => {
    const user = userEvent.setup();
    render(<TodoList />);
    await add(user, '  buy milk ');
    await add(user, 'write tests');
    const items = within(screen.getByRole('list')).getAllByRole('listitem');
    expect(items.map((li) => li.textContent)).toEqual([expect.stringContaining('buy milk'), expect.stringContaining('write tests')]);
    expect(screen.getByLabelText('New todo')).toHaveValue('');
    expect(screen.getByText('2 open')).toBeInTheDocument();
  });

  it('submits with Enter and ignores blank input', async () => {
    const user = userEvent.setup();
    render(<TodoList />);
    await user.type(screen.getByLabelText('New todo'), 'via enter{Enter}');
    await add(user, '   ');
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
  });

  it('toggles items done', async () => {
    const user = userEvent.setup();
    render(<TodoList />);
    await add(user, 'a');
    await add(user, 'b');
    await user.click(screen.getByRole('checkbox', { name: 'a' }));
    expect(screen.getByRole('checkbox', { name: 'a' })).toBeChecked();
    expect(screen.getByText('1 open')).toBeInTheDocument();
  });

  it('deletes the right item and keeps the others state (stable keys)', async () => {
    const user = userEvent.setup();
    render(<TodoList />);
    await add(user, 'first');
    await add(user, 'second');
    await add(user, 'third');
    await user.click(screen.getByRole('checkbox', { name: 'third' }));
    await user.click(screen.getByRole('button', { name: 'Delete first' }));
    expect(screen.queryByText('first')).not.toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'second' })).not.toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'third' })).toBeChecked();
    await user.click(screen.getByRole('button', { name: 'Delete second' }));
    await user.click(screen.getByRole('button', { name: 'Delete third' }));
    expect(screen.getByText('Nothing to do')).toBeInTheDocument();
  });
});
