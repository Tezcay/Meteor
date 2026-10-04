import { apiPost } from './http'

export type PaymentResponse = {
  eventId: number
  provider: string
  providerEventId: string
  providerTransactionId: string
  processingStatus: string
  orderId: number
  orderStatus: string
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
