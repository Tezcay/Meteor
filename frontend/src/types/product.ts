export interface PackageOption {
  id: number
  name: string
  priceCents: number
  currency: string
}

export interface ProductDetailResponse {
  id: number
  shopName: string
  title: string
  summary: string
  packages: PackageOption[]
}

export interface ProductListItemResponse {
  id: number
  shopName: string
  title: string
  summary: string
}
