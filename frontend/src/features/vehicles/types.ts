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

export interface RepairHistoryEntry {
  appointmentId: number
  appointmentReference: string
  visitDate: string
  repairDescription: string
  totalGrossAmount: number
  repairCompletedAt: string
  repairCompletedBy: string
  vehiclePickedUpAt: string
  vehiclePickedUpBy: string
}
