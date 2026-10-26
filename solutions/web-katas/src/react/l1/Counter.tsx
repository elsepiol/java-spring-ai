import { useState } from 'react';

/** Reference solution for the Counter kata. */
export interface CounterProps {
  initial?: number;
  min?: number;
  max?: number;
  onChange?: (value: number) => void;
}

export function Counter({ initial = 1, min = 1, max = 10, onChange }: CounterProps) {
  const [value, setValue] = useState(initial);

  const change = (next: number) => {
    setValue(next);
    onChange?.(next); // in the event handler, not in an effect: it is a reaction to the click
  };

  return (
    <div>
      <button type="button" onClick={() => change(value - 1)} disabled={value <= min}>
        Decrease
      </button>
      <output role="status">{value}</output>
      <button type="button" onClick={() => change(value + 1)} disabled={value >= max}>
        Increase
      </button>
      <button type="button" onClick={() => change(initial)}>
        Reset
      </button>
    </div>
  );
}
