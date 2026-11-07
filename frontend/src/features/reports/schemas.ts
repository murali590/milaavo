import { z } from 'zod'
import { REPORT_TYPES } from './reportTypes'

// Same rules as CreateReportRequest in the backend.
export const reportSchema = z.object({
  type: z.enum(REPORT_TYPES, { message: 'Choose a report type' }),
  itemName: z
    .string()
    .trim()
    .min(1, 'Item name is required')
    .max(200, 'Item name must be at most 200 characters'),
  description: z
    .string()
    .trim()
    .min(1, 'Description is required')
    .max(2000, 'Description must be at most 2000 characters'),
  city: z.string().trim().min(1, 'City is required').max(100, 'City must be at most 100 characters'),
})

export type ReportFormValues = z.infer<typeof reportSchema>