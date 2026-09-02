import { useEffect, useState } from 'react'
import { errorMessage } from '../../../api/apiClient'
import { getCatalog } from '../api/servicesApi'
import type { ServiceCategory } from '../types'

export function useServiceCatalog() {
  const [categories, setCategories] = useState<ServiceCategory[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    getCatalog(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setCategories(result)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false)
      })

    // Cancel an old request when refreshing or leaving the page.
    return () => controller.abort()
  }, [revision])

  function reload() {
    setIsLoading(true)
    setError(null)
    setRevision((value) => value + 1)
  }

  return { categories, isLoading, error, reload }
}
