import { Component, type ErrorInfo, type ReactNode } from 'react';

/** Reference solution for the ErrorBoundary kata. */
export interface ErrorBoundaryProps {
  children: ReactNode;
  fallback: (error: Error, reset: () => void) => ReactNode;
  onError?: (error: Error) => void;
  resetKeys?: unknown[];
}

interface State {
  error: Error | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: unknown): State {
    return { error: error instanceof Error ? error : new Error(String(error)) };
  }

  componentDidCatch(error: unknown, _info: ErrorInfo) {
    this.props.onError?.(error instanceof Error ? error : new Error(String(error)));
  }

  componentDidUpdate(previous: ErrorBoundaryProps) {
    if (this.state.error && changed(previous.resetKeys, this.props.resetKeys)) this.reset();
  }

  reset = () => this.setState({ error: null });

  render() {
    return this.state.error ? this.props.fallback(this.state.error, this.reset) : this.props.children;
  }
}

function changed(a: unknown[] = [], b: unknown[] = []) {
  return a.length !== b.length || a.some((x, i) => !Object.is(x, b[i]));
}
