import { apiGet, apiPost } from './http'

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'CLOSED' | 'REFUNDED'

export type OrderResponse = {
  id: number
  status: OrderStatus
  totalAmountCents: number
  currency: string
  idempotencyKey: string
  productId: number
  packageId: number
  productTitle: string
  shopName: string
  packageName: string
  accessPolicy: 'ALL_RELEASES'
}

export type OrderDetailResponse = {
  id: number
  status: OrderStatus
  totalAmountCents: number
  currency: string
  createdAt: string
  productId: number
  packageId: number
  productTitle: string
  shopName: string
  packageName: string
  accessPolicy: 'ALL_RELEASES'
  entitlementGrantId: number | null
  entitlementStatus: 'ACTIVE' | 'REVOKED' | null
  grantedAt: string | null
}

export async function createOrder(productId: number, packageId: number, idempotencyKey: string) {
  return apiPost<OrderResponse>('/api/orders', { productId, packageId, idempotencyKey }, true)
}

export async function getOrders(signal?: AbortSignal) {
  return apiGet<OrderDetailResponse[]>('/api/orders', signal)
}

export async function getOrder(orderId: number, signal?: AbortSignal) {
  return apiGet<OrderDetailResponse>(`/api/orders/${orderId}`, signal)
}
