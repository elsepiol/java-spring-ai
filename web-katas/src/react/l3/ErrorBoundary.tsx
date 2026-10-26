import type { ReactNode } from 'react';
import { todo } from '../../kata';

/**
 * KATA react L3 "ErrorBoundary" (tracks/13-react/L3-production.md, section 3).
 *
 * Without a boundary, an exception while rendering ANY component unmounts the WHOLE app (a white
 * page). Error boundaries are still class components (getDerivedStateFromError + componentDidCatch):
 * - render children normally; when a descendant throws during rendering, render
 *   fallback(error, reset) instead;
 * - call onError(error) once per caught error (the place to report it, e.g. to Sentry);
 * - reset() clears the error and renders the children again;
 * - when any value in resetKeys changes (e.g. the route or the selected id), reset automatically.
 * Replace this function with a class `ErrorBoundary extends Component<ErrorBoundaryProps, State>`.
 * (Errors in event handlers and async code are not caught by boundaries: handle those yourself.)
 */
export interface ErrorBoundaryProps {
  children: ReactNode;
  fallback: (error: Error, reset: () => void) => ReactNode;
  onError?: (error: Error) => void;
  resetKeys?: unknown[];
}

export function ErrorBoundary(props: ErrorBoundaryProps): ReactNode {
  return todo('tracks/13-react/L3-production.md section 3');
}
