export interface WorkshopService {
  id: number
  categoryId: number
  name: string
  description: string
}

export interface ServiceCategory {
  id: number
  name: string
  description: string
  services: WorkshopService[]
}

export interface CategoryInput {
  name: string
  description: string
}

export interface ServiceInput {
  categoryId: number
  name: string
  description: string
}
