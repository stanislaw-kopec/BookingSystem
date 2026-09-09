import { workshopInfo } from '../workshopInfo'

export function WorkshopLocation() {
  const { location } = workshopInfo
  const coordinates = `${location.latitude},${location.longitude}`
  const mapUrl = `https://www.google.com/maps?q=${encodeURIComponent(coordinates)}&z=14&output=embed`
  const directionsUrl = `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(coordinates)}`

  return (
    <section id="location" className="page-section workshop-location" aria-labelledby="location-heading">
      <div className="section-heading">
        <p className="eyebrow">Dojazd</p>
        <h2 id="location-heading">Lokalizacja warsztatu</h2>
      </div>
      <div className="workshop-location-layout">
        <div className="workshop-location-details">
          <h3>{location.label}</h3>
          <p className="muted">{location.description}</p>
          <a className="button secondary" href={directionsUrl} target="_blank" rel="noopener noreferrer">
            Wyznacz trasę w Google Maps
          </a>
        </div>
        <iframe className="workshop-map" title={`Mapa Google — przykładowa lokalizacja: ${location.label}`}
          src={mapUrl} loading="lazy" referrerPolicy="no-referrer-when-downgrade" allowFullScreen />
      </div>
    </section>
  )
}
