import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { CustomSelect } from './CustomSelect'

const options = [
  { value: 'first', label: 'Pierwsza' },
  { value: 'disabled', label: 'Niedostępna', disabled: true },
  { value: 'last', label: 'Ostatnia' },
]
function select(onChange = vi.fn()) {
  render(<><label htmlFor="test-select">Status</label><CustomSelect id="test-select" value="first" options={options} onChange={onChange} /><button>Dalej</button></>)
  return screen.getByRole('combobox', { name: 'Status' })
}

describe('CustomSelect', () => {
  it('does not report an unchanged selection and keeps focus on the combobox', async () => {
    const user = userEvent.setup()
    const change = vi.fn()
    const trigger = select(change)
    await user.click(trigger)
    await user.click(screen.getByRole('option', { name: 'Pierwsza' }))
    expect(change).not.toHaveBeenCalled()
    expect(trigger).toHaveFocus()
    expect(trigger).toHaveAttribute('aria-expanded', 'false')
  })

  it('announces and scrolls the active option, skips disabled options and selects with Enter', async () => {
    const user = userEvent.setup()
    const change = vi.fn()
    const trigger = select(change)
    await user.tab()
    await user.keyboard('{ArrowDown}{ArrowDown}')
    const last = screen.getByRole('option', { name: 'Ostatnia' })
    expect(trigger).toHaveFocus()
    expect(trigger).toHaveAttribute('aria-activedescendant', last.id)
    expect(HTMLElement.prototype.scrollIntoView).toHaveBeenCalledWith({ block: 'nearest' })
    expect(screen.getByRole('option', { name: 'Niedostępna' })).toHaveAttribute('aria-disabled', 'true')
    await user.keyboard('{Enter}')
    expect(change).toHaveBeenCalledWith('last')
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })

  it('supports Home, End, Escape, Tab and outside click without trapping focus', async () => {
    const user = userEvent.setup()
    const trigger = select()
    await user.click(trigger)
    await user.keyboard('{End}')
    expect(trigger).toHaveAttribute('aria-activedescendant', screen.getByRole('option', { name: 'Ostatnia' }).id)
    await user.keyboard('{Home}{Escape}')
    expect(trigger).toHaveFocus()
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    await user.keyboard(' {Tab}')
    expect(screen.getByRole('button', { name: 'Dalej' })).toHaveFocus()
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    await user.click(trigger)
    await user.click(screen.getByRole('button', { name: 'Dalej' }))
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })
})
