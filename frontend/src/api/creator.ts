import { apiGet, apiPost } from './http'

export type CreatorProduct = {
  productId: number
  revisionId: number
  title: string
  summary: string
  revisionStatus: string
  saleStatus: string
  createdAt: string
}
export type DeliveryAsset = { id: number; filename: string; mediaType: string; sizeBytes: number }
export type Publication = {
  productId: number
  packageId: number
  releaseId: number
  manifestId: number
  manifestItemId: number
}
export const getCreatorProducts = () => apiGet<CreatorProduct[]>('/api/creator/products')
export const getDeliveryAssets = () => apiGet<DeliveryAsset[]>('/api/creator/delivery-assets')
export const createProductDraft = (title: string, summary: string) =>
  apiPost<CreatorProduct>('/api/creator/product-drafts', { title, summary })
// POST 发布可重放，但前提是 productId 和配置不变；创建草稿的 POST 尚无幂等保证。
export const publishProduct = (
  productId: number,
  packageName: string,
  priceCents: number,
  fileObjectId: number,
) =>
  apiPost<Publication>(`/api/creator/product-drafts/${productId}/publish`, {
    packageName,
    priceCents,
    fileObjectId,
  })
