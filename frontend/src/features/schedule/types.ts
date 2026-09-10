export interface ScheduleSettings {
  defaultDailyCapacity: number
  bookingHorizonDays: number
  workdayStart: string
  workdayEnd: string
}

export interface ScheduleDayOverride {
  id: number
  date: string
  capacity: number
  closed: boolean
  note: string
}

export interface WorkshopScheduleConfig {
  timeZone: string
  settings: ScheduleSettings
  overrides: ScheduleDayOverride[]
}

export interface ScheduleDayOverrideInput {
  date: string
  capacity: number
  closed: boolean
  note: string
}
