import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { ReportTypePicker } from './ReportTypePicker'
import { REPORT_TYPE_INFO } from '../reportTypes'
import { reportSchema, type ReportFormValues } from '../schemas'
import { isApiError } from '../../../shared/api/ApiError'
import { ErrorCode } from '../../../shared/api/errorCodes'
import { applyServerFieldErrors } from '../../../shared/forms/applyServerFieldErrors'
import { Alert, Button, LinkButton, TextAreaField, TextField } from '../../../shared/ui'

const SERVER_FIELDS = ['type', 'itemName', 'description', 'city'] as const

type ReportFormProps = {
  defaultValues: ReportFormValues
  submitLabel: string
  cancelTo: string
  // Should throw an ApiError on failure; the form shows it.
  onSubmit: (values: ReportFormValues) => Promise<void>
}

export function ReportForm({ defaultValues, submitLabel, cancelTo, onSubmit }: ReportFormProps) {
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm<ReportFormValues>({
    resolver: zodResolver(reportSchema),
    mode: 'onTouched',
    defaultValues,
  })

  const type = useWatch({ control, name: 'type' })
  const description = useWatch({ control, name: 'description' })
  const typeInfo = REPORT_TYPE_INFO[type] ?? REPORT_TYPE_INFO.LOST

  async function submit(values: ReportFormValues) {
    setFormError(null)
    try {
      await onSubmit(values)
    } catch (error) {
      if (isApiError(error) && error.code === ErrorCode.VALIDATION_ERROR && applyServerFieldErrors(error, setError, SERVER_FIELDS)) {
        return
      }
      setFormError(isApiError(error) ? error.message : 'Something went wrong. Please try again.')
    }
  }

  return (
    <form className="space-y-5" onSubmit={handleSubmit(submit)} noValidate>
      {formError && <Alert tone="danger">{formError}</Alert>}

      <ReportTypePicker registration={register('type')} error={errors.type?.message} />

      <TextField
        label={typeInfo.itemLabel}
        placeholder="e.g. Black leather wallet"
        maxLength={200}
        error={errors.itemName?.message}
        {...register('itemName')}
      />
      <TextAreaField
        label="Description"
        placeholder={typeInfo.descriptionPlaceholder}
        rows={5}
        maxLength={2000}
        hint={`${description?.length ?? 0} / 2000`}
        error={errors.description?.message}
        {...register('description')}
      />
      <TextField
        label="City"
        placeholder="e.g. Bengaluru"
        autoComplete="address-level2"
        maxLength={100}
        error={errors.city?.message}
        {...register('city')}
      />

      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <LinkButton to={cancelTo} variant="secondary">
          Cancel
        </LinkButton>
        <Button type="submit" loading={isSubmitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}