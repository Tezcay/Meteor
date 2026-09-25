import type { DiscoveryCategory } from '../types/discovery'

export interface DiscoveryCategoryOption {
  value: Exclude<DiscoveryCategory, 'all'>
  label: string
}

export interface DiscoverySubcategoryOption {
  value: string
  label: string
}

export const categories: DiscoveryCategoryOption[] = [
  { value: 'design', label: '设计素材' },
  { value: 'templates', label: '实用模板' },
  { value: 'photography', label: '摄影' },
  { value: 'illustration', label: '插画' },
  { value: 'fonts', label: '字体' },
  { value: 'three-d', label: '3D 素材' },
  { value: 'audio', label: '音频' },
  { value: 'publications', label: '电子书' },
]

// Platform-maintained vocabulary. Creators select from it; they do not invent new browse groups.
export const categoryTaxonomy: Record<Exclude<DiscoveryCategory, 'all'>, DiscoverySubcategoryOption[]> = {
  design: [
    { value: 'layout', label: '版式与构成' },
    { value: 'identity', label: '品牌与标识' },
  ],
  templates: [
    { value: 'work', label: '工作与求职' },
    { value: 'planning', label: '计划与记录' },
  ],
  photography: [
    { value: 'space', label: '建筑与空间' },
    { value: 'people', label: '人物与日常' },
    { value: 'nature', label: '自然与风景' },
  ],
  illustration: [
    { value: 'landscape', label: '风景叙事' },
    { value: 'character', label: '人物与角色' },
  ],
  fonts: [
    { value: 'display', label: '标题字' },
    { value: 'text', label: '正文字体' },
  ],
  'three-d': [
    { value: 'object', label: '物件与材质' },
    { value: 'scene', label: '空间与场景' },
  ],
  audio: [
    { value: 'ambient', label: '氛围与环境' },
    { value: 'music', label: '音乐与节拍' },
  ],
  publications: [
    { value: 'visual', label: '视觉读物' },
    { value: 'writing', label: '写作与观察' },
  ],
}

export const categoryOptions: { value: DiscoveryCategory; label: string }[] = [
  { value: 'all', label: '全部' },
  ...categories,
]

const legacyCategoryAliases: Record<string, DiscoveryCategory> = {
  设计: 'design',
  模板: 'templates',
  摄影: 'photography',
  读物: 'publications',
}

export function parseCategory(value: string | null): DiscoveryCategory {
  if (!value) return 'all'
  const normalized = legacyCategoryAliases[value] ?? value
  return categoryOptions.some((item) => item.value === normalized)
    ? (normalized as DiscoveryCategory)
    : 'all'
}

export function categoryLabel(value: DiscoveryCategory): string {
  return categoryOptions.find((item) => item.value === value)?.label ?? '全部'
}

export function subcategoryLabel(category: Exclude<DiscoveryCategory, 'all'>, value: string): string {
  return categoryTaxonomy[category].find((item) => item.value === value)?.label ?? '其他'
}
