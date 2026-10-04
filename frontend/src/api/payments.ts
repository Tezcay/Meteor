import { apiPost } from './http'
import type { OrderStatus } from './orders'

export type PaymentEventProcessingStatus = 'APPLIED' | 'IGNORED' | 'REJECTED'

export type PaymentResponse = {
  eventId: number
  provider: string
  providerEventId: string
  providerTransactionId: string
  processingStatus: PaymentEventProcessingStatus
  orderId: number
  orderStatus: OrderStatus
  amountCents: number
  currency: string
  entitlementGrantId: number | null
  entitlementStatus: 'ACTIVE' | 'REVOKED' | null
}

export async function applyTestPayment(
  orderId: number,
  providerEventId: string,
  providerTransactionId: string,
  amountCents: number,
  currency: string,
) {
  return apiPost<PaymentResponse>(
    `/api/orders/${orderId}/test-payment`,
    {
      providerEventId,
      providerTransactionId,
      amountCents,
      currency,
    },
    true,
  )
}
