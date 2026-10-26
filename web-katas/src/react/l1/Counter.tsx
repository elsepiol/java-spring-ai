import { todo } from '../../kata';

/**
 * KATA react L1 "Counter" (tracks/13-react/L1-basics.md, section 3).
 *
 * A quantity picker, as on a product page:
 * - shows the current value in an element with role "status" (an <output> works), starting at
 *   `initial` (default 1);
 * - a button "Decrease" and a button "Increase" change it by 1, never below `min` (default 1)
 *   or above `max` (default 10): at a limit the button is DISABLED (not just ignored);
 * - a button "Reset" goes back to `initial`;
 * - every change calls onChange(newValue) if given (not on the first render).
 * State with useState; derive "at min"/"at max" from the value instead of storing them.
 */
export interface CounterProps {
  initial?: number;
  min?: number;
  max?: number;
  onChange?: (value: number) => void;
}

export function Counter(props: CounterProps) {
  return todo('tracks/13-react/L1-basics.md section 3');
}
