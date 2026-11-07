import { keepPreviousData, useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { createReport, fetchReport, fetchReports, updateReport, deleteReport } from './api'
import type { ReportType } from './reportTypes'
import type { ItemReport, PagedResponse, ReportInput } from './types'

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

// The report may already be in a cached list page: show it at once while the real request runs.
function findInCachedLists(queryClient: QueryClient, id: string): ItemReport | undefined {
    const pages = queryClient.getQueriesData<PagedResponse<ItemReport>>({ queryKey: reportKeys.lists() })
    for (const [, page] of pages) {
        const match = page?.content.find((report) => report.reportId === id)
        if (match) {
            return match
        }
    }
    return undefined
}

export function useReport(id: string) {
    const queryClient = useQueryClient()

    return useQuery<ItemReport>({
        queryKey: reportKeys.detail(id),
        queryFn: ({ signal }) => fetchReport(id, signal),
        placeholderData: () => findInCachedLists(queryClient, id),
    })
}

export function useUpdateReport() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ id, input }: { id: string; input: ReportInput }) => updateReport(id, input),
        onSuccess: (updated) => {
            // The PUT response is the new state: put it in the cache instead of refetching.
            queryClient.setQueryData(reportKeys.detail(updated.reportId), updated)
            // Lists show the name, type and city too, so they are out of date.
            return queryClient.invalidateQueries({ queryKey: reportKeys.lists() })
        },
    })
}

export function useDeleteReport() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deleteReport,
    // Gone from every list (order, totals and pages change).
    onSuccess: () => queryClient.invalidateQueries({ queryKey: reportKeys.lists() }),
  })
}