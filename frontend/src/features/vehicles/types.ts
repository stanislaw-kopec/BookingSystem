export interface VehicleInput {
  make: string
  model: string
  productionYear: number
  registrationNumber: string
  vin: string
}

export interface Vehicle extends VehicleInput {
  id: number
}

export type RepairItemType = 'LABOR' | 'PART'

export interface RepairItem {
  id: number | null
  type: RepairItemType
  name: string
  quantity: number
  unitGrossAmount: number
  totalGrossAmount: number
}

export interface RepairHistoryEntry {
  appointmentId: number
  appointmentReference: string
  visitDate: string
  repairDescription: string
  totalGrossAmount: number
  repairItems: RepairItem[]
  repairCompletedAt: string
  repairCompletedBy: string
  vehiclePickedUpAt: string
  vehiclePickedUpBy: string
}
