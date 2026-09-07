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
