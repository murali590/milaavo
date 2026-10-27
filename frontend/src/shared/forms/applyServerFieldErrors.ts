import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { ApiError } from '../api/ApiError'

// Puts the backend's fieldErrors under the matching form fields.
// Returns true if at least one error was placed, so the caller knows it was handled.
export function applyServerFieldErrors<T extends FieldValues>(
  error: ApiError,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): boolean {
  let applied = false

  for (const [field, message] of Object.entries(error.fieldErrors)) {
    if ((fields as readonly string[]).includes(field)) {
      setError(field as Path<T>, { type: 'server', message })
      applied = true
    }
  }

  return applied
}