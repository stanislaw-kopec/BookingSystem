import { useEffect, useId, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { useTranslation } from '../../i18n/useTranslation'

export interface CustomSelectOption {
  value: string
  label: string
  disabled?: boolean
}

interface Props {
  id: string
  value: string
  options: CustomSelectOption[]
  onChange: (value: string) => void
  placeholder?: string
  disabled?: boolean
  invalid?: boolean
  describedBy?: string
}

function firstEnabledIndex(options: CustomSelectOption[]) {
  return options.findIndex((option) => !option.disabled)
}

function nextEnabledIndex(options: CustomSelectOption[], currentIndex: number, direction: 1 | -1) {
  const enabledIndexes = options
    .map((option, index) => ({ option, index }))
    .filter(({ option }) => !option.disabled)
    .map(({ index }) => index)

  if (enabledIndexes.length === 0) return -1

  const currentPosition = enabledIndexes.indexOf(currentIndex)
  if (currentPosition === -1) return enabledIndexes[0]

  const nextPosition = (currentPosition + direction + enabledIndexes.length) % enabledIndexes.length
  return enabledIndexes[nextPosition]
}

export function CustomSelect({
  id,
  value,
  options,
  onChange,
  placeholder,
  disabled = false,
  invalid = false,
  describedBy,
}: Props) {
  const generatedId = useId()
  const listboxId = `${id || generatedId}-listbox`
  const wrapperRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const { t } = useTranslation()
  const [isOpen, setIsOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState(() => {
    const selectedIndex = options.findIndex((option) => option.value === value && !option.disabled)
    return selectedIndex >= 0 ? selectedIndex : firstEnabledIndex(options)
  })

  const selectedOption = options.find((option) => option.value === value)

  useEffect(() => {
    if (!isOpen) return

    function closeOnOutsideClick(event: PointerEvent) {
      if (!wrapperRef.current?.contains(event.target as Node)) setIsOpen(false)
    }

    document.addEventListener('pointerdown', closeOnOutsideClick)
    return () => document.removeEventListener('pointerdown', closeOnOutsideClick)
  }, [isOpen])

  function openList() {
    if (disabled) return
    const selectedIndex = options.findIndex((option) => option.value === value && !option.disabled)
    setActiveIndex(selectedIndex >= 0 ? selectedIndex : firstEnabledIndex(options))
    setIsOpen(true)
  }

  function selectOption(option: CustomSelectOption) {
    if (option.disabled) return
    onChange(option.value)
    setIsOpen(false)
    triggerRef.current?.focus()
  }

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (disabled) return

    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      if (!isOpen) {
        openList()
        return
      }
      setActiveIndex((current) => nextEnabledIndex(options, current, event.key === 'ArrowDown' ? 1 : -1))
      return
    }

    if (event.key === 'Home') {
      event.preventDefault()
      if (!isOpen) setIsOpen(true)
      setActiveIndex(firstEnabledIndex(options))
      return
    }

    if (event.key === 'End') {
      event.preventDefault()
      if (!isOpen) setIsOpen(true)
      setActiveIndex(nextEnabledIndex(options, firstEnabledIndex(options), -1))
      return
    }

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      if (!isOpen) {
        openList()
        return
      }
      const activeOption = options[activeIndex]
      if (activeOption) selectOption(activeOption)
      return
    }

    if (event.key === 'Escape') {
      setIsOpen(false)
      return
    }

    if (event.key === 'Tab') setIsOpen(false)
  }

  return (
    <div className={`custom-select${isOpen ? ' open' : ''}`} ref={wrapperRef}>
      <button id={id} ref={triggerRef} type="button" className="custom-select-trigger"
        aria-haspopup="listbox" aria-expanded={isOpen} aria-controls={listboxId}
        aria-invalid={invalid || undefined} aria-describedby={describedBy} disabled={disabled}
        onClick={() => isOpen ? setIsOpen(false) : openList()} onKeyDown={handleKeyDown}>
        <span className={selectedOption ? 'custom-select-value' : 'custom-select-value placeholder'}>
          {selectedOption?.label ?? placeholder ?? t('select.defaultPlaceholder')}
        </span>
      </button>
      {isOpen && (
        <ul id={listboxId} className="custom-select-panel" role="listbox" aria-labelledby={id}>
          {options.map((option, index) => (
            <li key={option.value} role="option" aria-selected={option.value === value}
              className={`custom-select-option${index === activeIndex ? ' active' : ''}${option.disabled ? ' disabled' : ''}`}
              onMouseEnter={() => !option.disabled && setActiveIndex(index)}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => selectOption(option)}>
              {option.label}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
