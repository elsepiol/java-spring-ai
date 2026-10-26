/** Every unsolved kata calls this, so its tests fail with a pointer to the lesson. */
export function todo(where: string): never {
  throw new Error(`TODO: this kata is not done yet. Read ${where}`);
}
