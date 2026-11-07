import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchReports } from './api'

const PAGE_SIZE = 10

// page starts from 1, as shown in the URL. The API counts from 0.
export function useReports(page: number) {
  const apiPage = page - 1

  return useQuery({
    queryKey: ['reports', 'list', { page: apiPage, size: PAGE_SIZE }],
    queryFn: ({ signal }) => fetchReports(apiPage, PAGE_SIZE, signal),
    placeholderData: keepPreviousData,
  })
}