import { http } from '../../shared/api/http'
import type { ReportType } from './reportTypes'
import type { ItemReport, PagedResponse, ReportInput } from './types'

export async function fetchReports(
  page: number,
  size: number,
  type: ReportType | undefined,
  signal?: AbortSignal,
): Promise<PagedResponse<ItemReport>> {
  // axios leaves out params that are undefined, so "no filter" sends no type at all.
  const { data } = await http.get<PagedResponse<ItemReport>>('/api/reports', {
    params: { page, size, type },
    signal,
  })
  return data
}

export async function createReport(body: ReportInput): Promise<ItemReport> {
  const { data } = await http.post<ItemReport>('/api/reports', body)
  return data
}