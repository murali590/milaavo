import { http } from '../../shared/api/http'
import type { LostReport, PagedResponse } from './types'

export async function fetchReports(
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<PagedResponse<LostReport>> {
  const { data } = await http.get<PagedResponse<LostReport>>('/api/reports', {
    params: { page, size },
    signal,
  })
  return data
}