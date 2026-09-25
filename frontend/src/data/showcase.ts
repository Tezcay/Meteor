import type { DiscoveryCategory } from '../types/discovery'

export interface ShowcaseItem {
  title: string
  category: Exclude<DiscoveryCategory, 'all'>
  maker: string
  image?: string
  art: 'photo' | 'poster' | 'demo'
  description: string
  mark: string
  variant: number
  featured?: boolean
  subcategory: string
  tags: string[]
  /** Explicit editorial relationship; never inferred from category or visual similarity. */
  collectionSlug?: string
}

export interface ArtworkCollection {
  slug: string
  title: string
  maker: string
  category: 'photography' | 'illustration'
  summary: string
  images: Array<{ src: string; alt: string }>
}

type DemoCategory = 'design' | 'templates' | 'fonts' | 'three-d' | 'audio' | 'publications'

const demoCollections: Array<{
  category: DemoCategory
  items: Array<[title: string, mark: string]>
}> = [
  {
    category: 'design',
    items: [
      ['色彩构成', 'FORM 01'],
      ['网格系统', 'GRID 12'],
      ['图形语言', 'SHAPE'],
      ['品牌符号', 'MARK'],
      ['版式练习', 'TYPE 08'],
      ['图标集合', 'ICON 24'],
    ],
  },
  {
    category: 'templates',
    items: [
      ['提案模板', 'PITCH'],
      ['社交媒体套件', 'SOCIAL'],
      ['作品集版式', 'PORTFOLIO'],
      ['项目计划表', 'PLANNER'],
      ['简历模板', 'RESUME'],
      ['演示文稿', 'SLIDES'],
    ],
  },
  {
    category: 'fonts',
    items: [
      ['北岸黑体', '永 Aa'],
      ['云岚宋体', '山月'],
      ['零度圆体', '00°'],
      ['新章标题体', '章'],
      ['行旅正文体', '行旅'],
      ['Mono 24', 'R24'],
    ],
  },
  {
    category: 'three-d',
    items: [
      ['柔体几何', '01'],
      ['玻璃器皿', '02'],
      ['模块空间', '03'],
      ['金属字形', '04'],
      ['软质图标', '05'],
      ['抽象场景', '06'],
    ],
  },
  {
    category: 'audio',
    items: [
      ['城市采样', '03:42'],
      ['夜行节拍', '04:18'],
      ['雨天环境声', '12:06'],
      ['轻爵士片段', '02:58'],
      ['电影氛围', '06:31'],
      ['合成器纹理', '05:20'],
    ],
  },
  {
    category: 'publications',
    items: [
      ['独立出版手册', 'No. 01'],
      ['日常观察', 'No. 02'],
      ['城市散步', 'No. 03'],
      ['植物笔记', 'No. 04'],
      ['视觉档案', 'No. 05'],
      ['创作者指南', 'No. 06'],
    ],
  },
]

const demoItems: ShowcaseItem[] = demoCollections.flatMap(({ category, items }) =>
  items.map(([title, mark], variant) => ({
    title,
    category,
    maker: '演示素材',
    art: 'demo',
    description: `${title}演示素材`,
    mark,
    variant,
    featured: variant < 2,
    subcategory:
      ({ design: 'layout', templates: 'work', fonts: 'display', 'three-d': 'object', audio: 'ambient', publications: 'visual' })[category] ?? 'other',
    tags: [category, variant % 2 ? '收藏' : '精选'],
  })),
)

const photographyImages = [
  '/images/courtyard-1280.webp',
  '/images/exhibition-1942.webp',
  '/images/creator-studio-1280.webp',
]
const photographyTitles = ['庭院光影', '展厅一角', '创作现场']
const photographyItems: ShowcaseItem[] = photographyTitles.map((title, variant) => ({
  title,
  category: 'photography',
  maker: '摄影演示',
  image: photographyImages[variant],
  art: 'photo',
  description: title,
  mark: `PHOTO ${String(variant + 1).padStart(2, '0')}`,
  variant,
  featured: variant < 2,
  subcategory: 'space',
  tags: ['建筑光影', '空间', '自然光'],
  // Demo relationship configured by the editor for this specific image set.
  collectionSlug: 'light-and-space',
}))

const posterImages: Array<[string, string, string]> = [
  ['冬日松林', '/images/posters/winter-forest.webp', '深蓝色冬日松林，远处透出暖色天光'],
  ['静谧建筑', '/images/posters/still-places.webp', '蓝天下由暖色几何体组成的建筑'],
  ['雪山湖畔', '/images/posters/tahoe-overlook.webp', '俯瞰雪山、湖泊与松林'],
  ['暮色原野', '/images/posters/sunset-field.webp', '暖色夕阳下的原野与人物'],
  ['雪地木屋', '/images/posters/winter-cabin.webp', '雪地木屋亮着暖光，背后是湖泊与群山'],
  ['山野秋色', '/images/posters/wild-places.webp', '雪山、深色松林与金色秋树'],
]
const posterItems: ShowcaseItem[] = posterImages.map(([title, image, description], variant) => ({
  title,
  category: 'illustration',
  maker: '编辑精选',
  image,
  art: 'poster',
  description,
  mark: `POSTER ${String(variant + 1).padStart(2, '0')}`,
  variant,
  featured: variant < 2,
  subcategory: 'landscape',
  tags: ['风景', '冬日', '纸本质感'],
  // This is a manually curated sequence, not an automatic "similar work" result.
  collectionSlug: 'winter-stillness',
}))

// Editorial demonstrations are browsing material, not purchasable product records.
export const showcaseItems: ShowcaseItem[] = [...demoItems, ...photographyItems, ...posterItems]

export const artworkCollections: ArtworkCollection[] = [
  {
    slug: 'light-and-space',
    title: '光与空间',
    maker: '摄影演示',
    category: 'photography',
    summary: '三张关于建筑表面、展览空间与工作台光线的观察',
    images: [
      { src: '/images/courtyard-1280.webp', alt: '树影落在浅色庭院与拱门上' },
      { src: '/images/exhibition-1942.webp', alt: '展览空间中的建筑细节' },
      { src: '/images/creator-studio-1280.webp', alt: '日光下摆着纸张与色卡的工作台' },
    ],
  },
  {
    slug: 'winter-stillness',
    title: '冬日静谧',
    maker: '编辑精选',
    category: 'illustration',
    summary: '六张以松林、雪线与黄昏光线组成的插画序列',
    images: posterImages.map(([title, src, alt]) => ({ src, alt: `${title}：${alt}` })),
  },
]

export function collectionSlugFor(item: ShowcaseItem) {
  return item.collectionSlug ?? ''
}

export function getArtworkCollection(slug: string) {
  return artworkCollections.find((collection) => collection.slug === slug)
}

export const seededProduct = {
  id: 1001,
  shopName: 'Northstar Design',
  title: '求职材料模板包',
  summary: '包含简历、求职信与作品集排版模板',
  packages: [
    { id: 2001, name: 'Basic', priceCents: 1900, currency: 'CNY' },
    { id: 2002, name: 'Pro', priceCents: 3900, currency: 'CNY' },
  ],
}

export function yuan(cents: number): string {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(cents / 100)
}
