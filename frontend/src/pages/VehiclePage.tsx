import { Navigate, useParams } from 'react-router-dom'
import { VehicleDetailsSection } from '../features/vehicles/components/VehicleDetailsSection'

export function VehiclePage() {
  const { vehicleId } = useParams()
  const parsedVehicleId = Number(vehicleId)

  if (!Number.isSafeInteger(parsedVehicleId) || parsedVehicleId <= 0) {
    return <Navigate to="/vehicles" replace />
  }

  return (
    <main className="page-content">
      <VehicleDetailsSection key={parsedVehicleId} vehicleId={parsedVehicleId} />
    </main>
  )
}
