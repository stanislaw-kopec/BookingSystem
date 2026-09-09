import logoUrl from '../../../assets/branding/mietek-customs-logo.png'
import { workshopInfo } from '../workshopInfo'

interface Props {
  className?: string
}

export function WorkshopLogo({ className }: Props) {
  return (
    <img src={logoUrl} alt={`${workshopInfo.name} — kaczka w czapeczce w stylu hot roda`}
      className={className} width={1536} height={1024} />
  )
}
