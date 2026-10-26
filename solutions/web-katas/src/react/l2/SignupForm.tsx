import { useState, type FormEvent } from 'react';

/** Reference solution for the SignupForm kata. */
export interface SignupData {
  email: string;
  password: string;
}

type Errors = Partial<Record<keyof SignupData, string>>;

function validate({ email, password }: SignupData): Errors {
  const errors: Errors = {};
  if (!email.includes('@')) errors.email = 'Enter a valid email';
  if (password.length < 12) errors.password = 'Use at least 12 characters';
  return errors;
}

export function SignupForm({ onSubmit }: { onSubmit: (data: SignupData) => Promise<void> }) {
  const [data, setData] = useState<SignupData>({ email: '', password: '' });
  const [errors, setErrors] = useState<Errors>({});
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  if (done) return <p role="status">Welcome!</p>;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const found = validate(data);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setSubmitting(true);
    setServerError(null);
    try {
      await onSubmit(data);
      setDone(true);
    } catch (err) {
      setServerError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={submit} noValidate>
      <label htmlFor="email">Email</label>
      <input
        id="email"
        type="email"
        value={data.email}
        onChange={(e) => setData({ ...data, email: e.target.value })}
        aria-invalid={errors.email ? 'true' : undefined}
        aria-describedby={errors.email ? 'email-error' : undefined}
      />
      {errors.email && <p id="email-error">{errors.email}</p>}

      <label htmlFor="password">Password</label>
      <input
        id="password"
        type="password"
        value={data.password}
        onChange={(e) => setData({ ...data, password: e.target.value })}
        aria-invalid={errors.password ? 'true' : undefined}
        aria-describedby={errors.password ? 'password-error' : undefined}
      />
      {errors.password && <p id="password-error">{errors.password}</p>}

      {serverError && <p role="alert">{serverError}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? 'Signing up…' : 'Sign up'}
      </button>
    </form>
  );
}
