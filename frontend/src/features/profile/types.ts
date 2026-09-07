export interface ClientProfileInput {
  firstName: string
  lastName: string
  phoneNumber: string
  contactEmail: string
  addressLine: string
  postalCode: string
  city: string
  hasCompanyData: boolean
  companyName: string
  taxId: string
  billingAddressLine: string
  billingPostalCode: string
  billingCity: string
}

export interface ClientProfile extends ClientProfileInput {
  configured: boolean
}
