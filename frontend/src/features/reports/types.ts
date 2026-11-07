import type { ReportType } from './reportTypes'

// Matches CreateReportResponse in the backend.
export type ItemReport = {
  reportId: string
  type: ReportType
  itemName: string
  description: string
  city: string
  status: string
  createdAt: string
}

// Matches PagedResponse in the backend. Page numbers start from 0 here.
export type PagedResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// Body for create and update (CreateReportRequest in the backend).
export type ReportInput = {
  type: ReportType
  itemName: string
  description: string
  city: string
}