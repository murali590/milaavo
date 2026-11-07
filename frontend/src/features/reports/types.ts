// Matches CreateReportResponse in the backend.
export type LostReport = {
  reportId: string
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