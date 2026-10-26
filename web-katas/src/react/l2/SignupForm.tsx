import { todo } from '../../kata';

/**
 * KATA react L2 "SignupForm" (tracks/13-react/L2-practical.md, section 3).
 *
 * A controlled, accessible form:
 * - fields "Email" and "Password" (real <label>s, so getByLabelText finds them), and a submit
 *   button "Sign up";
 * - validation on submit: email must contain "@" -> "Enter a valid email"; password at least
 *   12 characters -> "Use at least 12 characters". Show each message next to its field, link it
 *   with aria-describedby and mark the field aria-invalid="true"; do NOT call onSubmit;
 * - when valid: call onSubmit({ email, password }) (it returns a Promise); while it runs the
 *   button is disabled and reads "Signing up…" (no double submits);
 * - if onSubmit rejects with an Error, show its message in an element with role "alert" and
 *   enable the button again; on success show "Welcome!" (role "status") instead of the form.
 */
export interface SignupData {
  email: string;
  password: string;
}

export function SignupForm(props: { onSubmit: (data: SignupData) => Promise<void> }) {
  return todo('tracks/13-react/L2-practical.md section 3');
}
