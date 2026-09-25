# 首页展示素材

2026-09-20 根据用户确认的无句号、无商品价格展览设计制作

- `exhibition-1942.webp`：1942 × 809，约 91 KB
- `exhibition-960.webp`：960 × 400，约 31 KB
- 使用内置 imagegen 编辑工具，从已确认的概念图提取纯展台场景
- WebP 为同一图片的尺寸/格式优化，图片不包含网站导航、按钮或页面标题
- 场景中的文字是画作自身内容，三幅图均为装饰素材，不代表已上架商品
- 页面不依赖生成工具的缓存路径，发布时复制 `public/` 即可
- `courtyard-640.webp` / `courtyard-1280.webp`：640 × 400 / 1280 × 801，约 44 / 151 KB，摄影分类的庭院场景
- `creator-studio-640.webp` / `creator-studio-1280.webp`：640 × 400 / 1280 × 801，约 39 / 110 KB，创作者介绍的工作台场景
- 新增两组场景使用内置 imagegen 生成，再优化为响应式 WebP，均在滚动接近时延迟加载
- 设计、读物、模板分类的装饰插画由 HTML/CSS/内联 SVG 绘制，不是实际商品封面

## 生成提示

Use case: precise-object-edit. Extract the approved gallery scene from the homepage mockup into a standalone high-resolution website hero image asset, wide landscape about 12:5. Remove all website interface elements: no header, navigation, hero heading, subheading, button, footer categories, prices or product captions. Output only the photographic exhibition scene. Preserve the left upright photograph of white stairs and an olive tree, the larger central Soft Forms poster with lilac and dark green shapes and a yellow sphere, and the right off-white book with plum botanical art. Preserve the curved pale stone shelf, olive branches at far left, small stones and book prop at far right, and natural soft morning daylight and leaf shadows. Retain text inside the artwork only. Keep all three artworks fully visible, center poster largest, camera straight-on, quiet warm-white surroundings. No browser frame or mockup UI.

## 下方场景生成提示

Photography: Landscape 16:10 editorial photograph for a quiet Chinese creative marketplace. Minimalist Mediterranean art museum courtyard, warm white curved stucco walls, pale stone steps, olive branches at upper left, delicate leaf shadows and pale sage sky. Realistic material and film photography, no people, logos, text, UI or product frames. Standalone category illustration.

Creator studio: Landscape 16:10 editorial photograph. An independent graphic artist's desk with a laptop displaying abstract lilac and forest-green art without readable text, a sketchbook of graphite botanical drawings, pencils, plum/lilac/sage colour swatches and ceramic cup. Elevated oblique view, sparse composition, natural daylight, leaf shadows and realistic material texture. No people, logos or website UI.

Homepage posters: `posters/` contains user-provided 3:4 artwork converted to optimized WebP files. The homepage carousel shows the previous, current and next poster on a neutral display surface, so light and dark artwork can share one system. Add a new asset there and register it in `src/components/HeroSection.vue`; the counter and cyclic navigation update from the array length.

## 字体信息

标题采用 LXGW WenKai TC Regular，仅保留“逛逛，也许正好喜欢”所需字符
字体由 Google Fonts 提供，使用 SIL Open Font License 1.1
源项目：https://github.com/lxgw/LxgwWenkaiTC
许可证随项目保存于 `../fonts/OFL.txt`
