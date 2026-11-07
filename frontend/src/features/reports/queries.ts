import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createReport, fetchReports } from './api'
import type { ReportType } from './reportTypes'

const PAGE_SIZE = 10

// Every reports cache key comes from here, so invalidation can never miss because of a typo.
export const reportKeys = {
  all: ['reports'] as const,
  lists: () => [...reportKeys.all, 'list'] as const,
  list: (page: number, size: number, type: ReportType | undefined) =>
    [...reportKeys.lists(), { page, size, type: type ?? 'ALL' }] as const,
  details: () => [...reportKeys.all, 'detail'] as const,
  detail: (id: string) => [...reportKeys.details(), id] as const,
}

// page starts from 1, as shown in the URL. The API counts from 0.
export function useReports(page: number, type: ReportType | undefined) {
  const apiPage = page - 1

  return useQuery({
    queryKey: reportKeys.list(apiPage, PAGE_SIZE, type),
    queryFn: ({ signal }) => fetchReports(apiPage, PAGE_SIZE, type, signal),
    placeholderData: keepPreviousData,
  })
}

export function useCreateReport() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createReport,
    // A new report changes every list (each filter, order and totals): mark them all stale.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: reportKeys.lists() }),
  })
}