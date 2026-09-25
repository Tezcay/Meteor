import type { ProductDetailResponse, ProductListItemResponse } from '../types/product'

export async function getPublishedProducts(
  query = '',
  category?: string,
  signal?: AbortSignal,
): Promise<ProductListItemResponse[]> {
  const params = new URLSearchParams()
  if (query.trim()) params.set('q', query.trim())
  if (category) params.set('category', category)
  const response = await fetch(`/api/products${params.size ? `?${params}` : ''}`, {
    headers: { Accept: 'application/json' },
    signal,
  })

  if (!response.ok) {
    throw new Error(`作品列表读取失败（HTTP ${response.status}）`)
  }

  return response.json() as Promise<ProductListItemResponse[]>
}

export async function getProductDetail(
  id: number,
  signal?: AbortSignal,
): Promise<ProductDetailResponse> {
  const response = await fetch(`/api/products/${id}`, {
    headers: { Accept: 'application/json' },
    signal,
  })

  if (!response.ok) {
    throw new Error(`商品读取失败（HTTP ${response.status}）`)
  }

  return response.json() as Promise<ProductDetailResponse>
}
