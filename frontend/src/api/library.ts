import { apiGet, apiPost, ensureOk } from './http'

export type LibraryItemResponse = {
  grantId: number
  orderId: number
  orderItemId: number
  productId: number
  packageId: number
  productTitle: string
  shopName: string
  packageName: string
  accessPolicy: 'ALL_RELEASES'
  grantStatus: 'ACTIVE'
  grantedAt: string
}

export type ReleaseResponse = {
  id: number
  packageId: number
  releaseNumber: number
  versionLabel: string
  summary: string
  publishedAt: string
  latest: boolean
}

export type ManifestItemResponse = {
  id: number
  position: number
  role: 'PRIMARY' | 'GUIDE' | 'BONUS'
  displayName: string
  filename: string
  mediaType: string
  sizeBytes: number
  sha256: string
}

export type DeliveryManifestResponse = {
  id: number
  releaseId: number
  versionLabel: string
  createdAt: string
  itemCount: number
  totalBytes: number
  items: ManifestItemResponse[]
}

export type DownloadAuthorizationResponse = {
  ticketId: number
  downloadUrl: string
  expiresAt: string
  filename: string
}

export async function getLibrary(signal?: AbortSignal) {
  return apiGet<LibraryItemResponse[]>('/api/library', signal)
}

export async function getAccessibleReleases(grantId: number, signal?: AbortSignal) {
  return apiGet<ReleaseResponse[]>(`/api/library/${grantId}/releases`, signal)
}

export async function getReleaseManifest(grantId: number, releaseId: number, signal?: AbortSignal) {
  return apiGet<DeliveryManifestResponse>(
    `/api/library/${grantId}/releases/${releaseId}/manifest`,
    signal,
  )
}

export async function createDownloadAuthorization(
  grantId: number,
  releaseId: number,
  manifestItemId: number,
) {
  return apiPost<DownloadAuthorizationResponse>(
    `/api/library/${grantId}/releases/${releaseId}/manifest/items/${manifestItemId}/download-authorizations`,
  )
}

export async function getAuthorizedDownload(downloadUrl: string) {
  const response = await fetch(downloadUrl, { credentials: 'include' })
  await ensureOk(response)
  return response.blob()
}
