import type { ServiceCategory } from '../types'

interface Props {
  category: ServiceCategory
}

export function ServiceCategoryCard({ category }: Props) {
  return (
    <article className="category-card">
      <h3>{category.name}</h3>
      {category.description && <p className="muted">{category.description}</p>}
      {category.services.length === 0 ? (
        <p className="empty-state">Brak usług w tej kategorii.</p>
      ) : (
        <ul className="service-list">
          {category.services.map((service) => (
            <li key={service.id}>
              <h4>{service.name}</h4>
              {service.description && <p>{service.description}</p>}
            </li>
          ))}
        </ul>
      )}
    </article>
  )
}
