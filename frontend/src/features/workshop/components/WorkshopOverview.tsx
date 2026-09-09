import { workshopInfo } from '../workshopInfo'
import { WorkshopLogo } from './WorkshopLogo'

export function WorkshopOverview() {
  return (
    <section id="about" className="page-section workshop-overview" aria-labelledby="workshop-heading">
      <div className="workshop-introduction">
        <div>
          <p className="eyebrow">O warsztacie</p>
          <h1 id="workshop-heading">{workshopInfo.name}</h1>
          <p className="lead">{workshopInfo.description}</p>
          <p className="muted">{workshopInfo.introduction}</p>
        </div>
        <WorkshopLogo className="workshop-hero-logo" />
      </div>
      <div className="workshop-history">
        <h2>Historia warsztatu</h2>
        {workshopInfo.history.map((paragraph) => (
          <p className="muted" key={paragraph}>{paragraph}</p>
        ))}
      </div>
      <a className="button secondary" href="#services">Zobacz usługi</a>
    </section>
  )
}
