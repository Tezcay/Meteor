export type CreatorProfile = {
  displayName: string
  introduction: string
}

export type LocalWorkStatus = 'draft' | 'review' | 'published' | 'rejected'

export type LocalWork = {
  id: string
  title: string
  summary: string
  version: string
  packageName: string
  files: string[]
  category: string
  subcategory: string
  tags: string[]
  status: LocalWorkStatus
  updatedAt: string
}

export type LocalPurchase = {
  productId: number
  title: string
  shopName: string
  packageName: string
  purchasedAt: string
}

export type LocalRole = 'visitor' | 'user' | 'creator' | 'admin'

type LocalState = {
  profile: CreatorProfile | null
  works: LocalWork[]
  purchase: LocalPurchase | null
  role: LocalRole
}
const storageKey = 'meteor.local-demo.v1'

function readState(): LocalState {
  try {
    const value = localStorage.getItem(storageKey)
    if (!value) return { profile: null, works: [], purchase: null, role: 'visitor' }
    const parsed = JSON.parse(value) as Partial<LocalState>
    return {
      profile: parsed.profile?.displayName ? (parsed.profile as CreatorProfile) : null,
      works: Array.isArray(parsed.works) ? (parsed.works as LocalWork[]) : [],
      purchase: parsed.purchase?.productId ? (parsed.purchase as LocalPurchase) : null,
      role: ['visitor', 'user', 'creator', 'admin'].includes(String(parsed.role))
        ? (parsed.role as LocalRole)
        : 'visitor',
    }
  } catch {
    return { profile: null, works: [], purchase: null, role: 'visitor' }
  }
}

function writeState(state: LocalState) {
  localStorage.setItem(storageKey, JSON.stringify(state))
}

export function getLocalProfile() {
  return readState().profile
}

export function saveLocalProfile(profile: CreatorProfile) {
  const state = readState()
  state.profile = profile
  writeState(state)
}

export function getLocalWorks() {
  return readState().works
}

export function saveLocalWork(
  input: Omit<LocalWork, 'id' | 'status' | 'updatedAt'>,
  status: LocalWorkStatus = 'draft',
) {
  const state = readState()
  const work: LocalWork = {
    ...input,
    id: `local-${Date.now()}`,
    status,
    updatedAt: new Date().toISOString(),
  }
  state.works = [work, ...state.works]
  writeState(state)
  return work
}

export function getLocalPurchase() {
  return readState().purchase
}

export function saveLocalPurchase(purchase: Omit<LocalPurchase, 'purchasedAt'>) {
  const state = readState()
  const nextPurchase: LocalPurchase = { ...purchase, purchasedAt: new Date().toISOString() }
  state.purchase = nextPurchase
  writeState(state)
  return nextPurchase
}

export function getLocalRole() {
  return readState().role
}

export function saveLocalRole(role: LocalRole) {
  const state = readState()
  state.role = role
  writeState(state)
}
