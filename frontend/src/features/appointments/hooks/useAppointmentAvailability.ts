import { useEffect, useState } from 'react'
import { errorMessage } from '../../../api/apiClient'
import * as appointmentsApi from '../api/appointmentsApi'
import type { AppointmentAvailability } from '../types'

export function useAppointmentAvailability() {
  const [availability, setAvailability] = useState<AppointmentAvailability | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    appointmentsApi.getAvailability(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          setAvailability(result)
          setError(null)
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false)
      })
    return () => controller.abort()
  }, [revision])

  function refresh() {
    setIsLoading(true)
    setError(null)
    setRevision((value) => value + 1)
  }

  return { availability, isLoading, error, refresh }
}
