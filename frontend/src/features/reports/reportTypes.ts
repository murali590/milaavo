// Must match ReportType in the backend.
export const REPORT_TYPES = ['LOST', 'FOUND', 'SWAPPED'] as const

export type ReportType = (typeof REPORT_TYPES)[number]

// For values from the URL: anything unknown means "no filter".
export function parseReportType(value: string | null): ReportType | undefined {
  return REPORT_TYPES.find((type) => type === value)
}

type ReportTypeInfo = {
  label: string
  summary: string
  badgeTone: 'lost' | 'found' | 'brand'
  itemLabel: string
  descriptionPlaceholder: string
}

export const REPORT_TYPE_INFO: Record<ReportType, ReportTypeInfo> = {
  LOST: {
    label: 'Lost',
    summary: 'I lost something',
    badgeTone: 'lost',
    itemLabel: 'What did you lose?',
    descriptionPlaceholder: 'Where and when did you lose it? Any marks that make it easy to recognise?',
  },
  FOUND: {
    label: 'Found',
    summary: "I found someone's item",
    badgeTone: 'found',
    itemLabel: 'What did you find?',
    descriptionPlaceholder: "Where and when did you find it? Don't share hidden details like ID numbers here.",
  },
  SWAPPED: {
    label: 'Swapped',
    summary: 'I have their item, they have mine',
    badgeTone: 'brand',
    itemLabel: 'Your item (the one you need back)',
    descriptionPlaceholder: 'Describe your item and the one you have now. Where and when did the mix-up happen?',
  },
}