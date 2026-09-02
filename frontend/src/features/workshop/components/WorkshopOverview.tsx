import { workshopInfo } from '../workshopInfo'

export function WorkshopOverview() {
  return (
    <section id="o-warsztacie" className="page-section workshop-overview" aria-labelledby="workshop-heading">
      <p className="eyebrow">Warsztat samochodowy</p>
      <h1 id="workshop-heading">{workshopInfo.name}</h1>
      <p className="lead">{workshopInfo.description}</p>
      <p className="muted">{workshopInfo.introduction}</p>
      <div className="workshop-details">
        <article>
          <h2>O warsztacie</h2>
          <p>{workshopInfo.history}</p>
        </article>
        <article id="lokalizacja">
          <h2>Lokalizacja</h2>
          <p>{workshopInfo.address}</p>
        </article>
      </div>
      <a className="button secondary" href="#uslugi">Zobacz usługi</a>
    </section>
  )
}
