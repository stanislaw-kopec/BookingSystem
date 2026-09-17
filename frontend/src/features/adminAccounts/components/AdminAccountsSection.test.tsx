import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it, vi } from 'vitest'
import * as api from '../api/adminAccountsApi'
import { AdminAccountsSection } from './AdminAccountsSection'

vi.mock('../api/adminAccountsApi')
it('does not show old accounts or a false empty state after filtering fails', async () => {
  const user = userEvent.setup()
  vi.mocked(api.getAccounts).mockResolvedValueOnce({
    content: [{ id: 1, username: 'old-client', email: 'test@example.com', role: 'CLIENT', enabled: true }],
    page: 0, size: 8, totalPages: 1, totalElements: 1,
  }).mockRejectedValueOnce(new Error('offline'))
  render(<AdminAccountsSection />)
  await screen.findByText('old-client')
  await user.click(screen.getByRole('combobox', { name: 'Rola' }))
  await user.click(screen.getByRole('option', { name: 'Wszystkie' }))
  expect(api.getAccounts).toHaveBeenCalledTimes(1)
  await user.click(screen.getByRole('combobox', { name: 'Rola' }))
  await user.click(screen.getByRole('option', { name: 'Mechanicy' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Nie udało się połączyć')
  expect(screen.queryByText('old-client')).not.toBeInTheDocument()
  expect(screen.queryByText('Nie znaleziono kont spełniających wybrane kryteria.')).not.toBeInTheDocument()
  vi.mocked(api.getAccounts).mockResolvedValueOnce({ content: [], page: 0, size: 8, totalPages: 0, totalElements: 0 })
  await user.click(screen.getByRole('button', { name: 'Szukaj' }))
  expect(await screen.findByText('Nie znaleziono kont spełniających wybrane kryteria.')).toBeInTheDocument()
})
