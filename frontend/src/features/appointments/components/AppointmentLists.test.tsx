import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../api/apiClient'
import * as api from '../api/appointmentsApi'
import type { Appointment, AppointmentPage } from '../types'
import { ClientAppointmentsSection } from './ClientAppointmentsSection'
import { StaffAppointmentsSection } from './StaffAppointmentsSection'

vi.mock('../api/appointmentsApi')
const appointment: Appointment = {
  id: 1, reference: 'TEST-1', requesterType: 'CLIENT', status: 'CONFIRMED', vehicleId: 1,
  vehicleMake: 'Honda', vehicleModel: 'City', vehicleProductionYear: 2020, vehicleRegistrationNumber: 'TEST123', vehicleVin: '',
  firstName: 'Anna', lastName: 'Test', phoneNumber: '123456789', contactEmail: 'test@example.com',
  requestedStartAt: '2026-09-21T06:00:00Z', currentStartAt: '2026-09-21T06:00:00Z',
  problemDescription: 'Problem z silnikiem', staffMessage: '', createdAt: '2026-09-17T06:00:00Z',
  staffActionAt: null, staffActionBy: null, clientConfirmedAt: null, repairDescription: '',
  totalGrossAmount: null, repairItems: [], repairCompletedAt: null, repairCompletedBy: null,
  vehiclePickedUpAt: null, vehiclePickedUpBy: null,
}
const page: AppointmentPage = { content: [appointment], page: 0, size: 5, totalElements: 1, totalPages: 1 }
beforeEach(() => {
  vi.mocked(api.getClientAppointments).mockReset().mockResolvedValue(page)
  vi.mocked(api.getStaffAppointments).mockReset().mockResolvedValue(page)
  vi.mocked(api.getAvailability).mockResolvedValue({ timeZone: 'Europe/Warsaw', dailyCapacity: 4, days: [] })
})

for (const [name, Component, load] of [
  ['client', ClientAppointmentsSection, api.getClientAppointments],
  ['staff', StaffAppointmentsSection, api.getStaffAppointments],
] as const) {
  describe(`${name} appointment list`, () => {
    it('does not stay loading when the same status or date order is selected', async () => {
      const user = userEvent.setup()
      render(<MemoryRouter><Component /></MemoryRouter>)
      await screen.findByText('Zgłoszenie TEST-1')
      for (const [label, option] of [['Status', 'Wszystkie statusy'], ['Sortowanie po dacie', 'Od najnowszych']]) {
        await user.click(screen.getByRole('combobox', { name: label }))
        await user.click(screen.getByRole('option', { name: option }))
      }
      expect(load).toHaveBeenCalledTimes(1)
      expect(screen.queryByText('Ładowanie zgłoszeń…')).not.toBeInTheDocument()
      expect(screen.getByText('Zgłoszenie TEST-1')).toBeInTheDocument()
    })

    it('hides old results on failure and retries the selected filter', async () => {
      const user = userEvent.setup()
      render(<MemoryRouter><Component /></MemoryRouter>)
      await screen.findByText('Zgłoszenie TEST-1')
      vi.mocked(load).mockRejectedValueOnce(new Error('offline'))
      await user.click(screen.getByRole('combobox', { name: 'Status' }))
      await user.click(screen.getByRole('option', { name: 'Zakończone' }))
      expect(await screen.findByRole('alert')).toHaveTextContent('Nie udało się połączyć')
      expect(screen.queryByText('Zgłoszenie TEST-1')).not.toBeInTheDocument()
      expect(screen.queryByText(/Pokazuję/)).not.toBeInTheDocument()
      vi.mocked(load).mockResolvedValueOnce({ ...page, content: [], totalElements: 0, totalPages: 0 })
      await user.click(screen.getByRole('button', { name: 'Spróbuj ponownie' }))
      expect(await screen.findByText('Brak zgłoszeń pasujących do wybranego statusu.')).toBeInTheDocument()
      expect(load).toHaveBeenLastCalledWith(0, 5, 'DESC', 'COMPLETED', expect.any(AbortSignal))
    })

    it('ignores an older response after a faster filter change', async () => {
      const user = userEvent.setup()
      render(<MemoryRouter><Component /></MemoryRouter>)
      await screen.findByText('Zgłoszenie TEST-1')
      let resolveOld!: (value: AppointmentPage) => void
      vi.mocked(load).mockImplementationOnce(() => new Promise((resolve) => { resolveOld = resolve }))
      await user.click(screen.getByRole('combobox', { name: 'Status' }))
      await user.click(screen.getByRole('option', { name: 'Zakończone' }))
      vi.mocked(load).mockResolvedValueOnce({ ...page, content: [], totalElements: 0, totalPages: 0 })
      await user.click(screen.getByRole('combobox', { name: 'Status' }))
      await user.click(screen.getByRole('option', { name: 'Odrzucone' }))
      await screen.findByText('Brak zgłoszeń pasujących do wybranego statusu.')
      await act(async () => resolveOld(page))
      expect(screen.queryByText('Zgłoszenie TEST-1')).not.toBeInTheDocument()
    })
  })
}

it('shows a Polish error linked to the quantity of the correct repair item', async () => {
  const user = userEvent.setup()
  render(<MemoryRouter><StaffAppointmentsSection /></MemoryRouter>)
  await user.click(await screen.findByRole('button', { name: 'Praca zakończona' }))
  await user.type(screen.getByLabelText('Wykonane prace'), 'Wymieniono uszkodzone części')
  await user.type(screen.getByLabelText('Nazwa'), 'Wymiana')
  await user.type(screen.getByLabelText('Cena brutto'), '50')
  vi.mocked(api.completeRepair).mockRejectedValueOnce(new ApiError(400, 'Invalid', {
    'repairItems[0].quantity': 'Quantity can have at most 6 integer digits and 2 decimal places.',
  }, 'APPOINTMENT_VALIDATION_FAILED'))
  await user.click(screen.getByRole('button', { name: 'Zamknij zgłoszenie' }))
  await screen.findByText(/Podaj ilość do 999/)
  expect(screen.getByLabelText('Ilość')).toHaveAttribute('aria-invalid', 'true')
  expect(screen.getByLabelText('Ilość')).toHaveAccessibleDescription(/Podaj ilość do 999/)
  expect(screen.getByRole('group', { name: 'Pozycja naprawy 1' })).toHaveTextContent('Podaj ilość')
})
