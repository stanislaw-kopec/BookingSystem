import { useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { I18nContext } from './I18nContext'
import type { I18nContextValue } from './I18nContext'
import { defaultLanguage, translations } from './translations'
import type { Language, TranslationKey } from './translations'

function storedLanguage(): Language {
  const value = localStorage.getItem('booking-system-language')
  return value === 'pl' || value === 'en' ? value : defaultLanguage
}

function translate(language: Language, key: TranslationKey, params: Record<string, string | number> = {}) {
  let text: string = translations[language][key]
  for (const [name, value] of Object.entries(params)) {
    text = text.replaceAll(`{${name}}`, String(value))
  }
  return text
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(storedLanguage)

  const value = useMemo<I18nContextValue>(() => ({
    language,
    setLanguage(nextLanguage) {
      localStorage.setItem('booking-system-language', nextLanguage)
      setLanguageState(nextLanguage)
    },
    t(key, params) {
      return translate(language, key, params)
    },
  }), [language])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}
