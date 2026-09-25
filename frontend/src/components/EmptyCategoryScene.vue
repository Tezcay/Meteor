<script setup lang="ts">
import { computed, ref } from 'vue'
import type { DiscoveryCategory } from '../types/discovery'

const props = defineProps<{
  category: DiscoveryCategory
}>()

defineEmits<{ reset: [] }>()

const motion = ref({
  '--shift-x': '0px',
  '--shift-y': '0px',
  '--tilt-x': '0deg',
  '--tilt-y': '0deg',
  '--spot-x': '50%',
  '--spot-y': '45%',
})

const palettes: Partial<Record<DiscoveryCategory, { accent: string; soft: string; glow: string }>> = {
  design: { accent: '#71445d', soft: '#d8df9d', glow: '#f1d7e5' },
  photography: { accent: '#4f6570', soft: '#cbd7d6', glow: '#dce9ee' },
  illustration: { accent: '#76577f', soft: '#d8cbe0', glow: '#f0d9c5' },
  fonts: { accent: '#2d2d2b', soft: '#d9d6cd', glow: '#e9dcc1' },
  'three-d': { accent: '#526553', soft: '#ced9bd', glow: '#e5d0b0' },
  audio: { accent: '#704c63', soft: '#d8c2cf', glow: '#ead9b5' },
  publications: { accent: '#4d5d72', soft: '#cbd4df', glow: '#ead8c8' },
}

const sceneStyle = computed(() => {
  const palette = palettes[props.category] ?? palettes.design!
  return {
    ...motion.value,
    '--scene-accent': palette.accent,
    '--scene-soft': palette.soft,
    '--scene-glow': palette.glow,
  }
})

function move(event: PointerEvent) {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const x = (event.clientX - rect.left) / rect.width
  const y = (event.clientY - rect.top) / rect.height
  motion.value = {
    '--shift-x': `${(x - 0.5) * 22}px`,
    '--shift-y': `${(y - 0.5) * 16}px`,
    '--tilt-x': `${(0.5 - y) * 3}deg`,
    '--tilt-y': `${(x - 0.5) * 4}deg`,
    '--spot-x': `${x * 100}%`,
    '--spot-y': `${y * 100}%`,
  }
}

function resetMotion() {
  motion.value = {
    '--shift-x': '0px',
    '--shift-y': '0px',
    '--tilt-x': '0deg',
    '--tilt-y': '0deg',
    '--spot-x': '50%',
    '--spot-y': '45%',
  }
}
</script>

<template>
  <section
    class="empty-gallery"
    :style="sceneStyle"
    aria-labelledby="empty-category-title"
    @pointermove="move"
    @pointerleave="resetMotion"
  >
    <div class="empty-gallery-light" aria-hidden="true"></div>
    <div class="empty-gallery-scene" aria-hidden="true">
      <div class="empty-gallery-shadow"></div>
      <div class="empty-gallery-orbit"></div>
      <div class="empty-gallery-paper paper-back"></div>
      <div class="empty-gallery-paper paper-middle">
        <span></span><span></span><span></span>
      </div>
      <div class="empty-gallery-paper paper-front">
        <span class="paper-rule"></span>
        <span class="paper-arch"></span>
        <span class="paper-circle"></span>
        <span class="paper-square"></span>
      </div>
      <span class="empty-gallery-dot dot-one"></span>
      <span class="empty-gallery-dot dot-two"></span>
    </div>

    <div class="empty-gallery-copy">
      <h2 id="empty-category-title">暂无作品</h2>
      <button class="empty-gallery-action" type="button" @click="$emit('reset')">
        查看全部
      </button>
    </div>
  </section>
</template>

<style scoped>
.empty-gallery {
  --scene-accent: #71445d;
  --scene-soft: #d8df9d;
  --scene-glow: #f1d7e5;
  --empty-surface: #f1f1eb;
  --empty-paper: #f7f6f0;
  --empty-paper-edge: #fbfaf6;
  --empty-shadow: rgb(44 42 38 / 7%);
  position: relative;
  min-height: 410px;
  margin-top: 32px;
  overflow: hidden;
  border: 1px solid var(--line);
  background: var(--empty-surface);
  isolation: isolate;
}
.empty-gallery:before,
.empty-gallery:after {
  content: '';
  position: absolute;
  width: 18px;
  height: 18px;
  border-color: color-mix(in srgb, var(--scene-accent) 58%, transparent);
  pointer-events: none;
}
.empty-gallery:before {
  top: 20px;
  left: 20px;
  border-top: 1px solid;
  border-left: 1px solid;
}
.empty-gallery:after {
  right: 20px;
  bottom: 20px;
  border-right: 1px solid;
  border-bottom: 1px solid;
}
.empty-gallery-light {
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    radial-gradient(
      circle at var(--spot-x) var(--spot-y),
      color-mix(in srgb, var(--scene-glow) 34%, transparent) 0,
      transparent 28%
    ),
    linear-gradient(115deg, transparent 58%, rgb(255 255 255 / 34%));
  transition: background 160ms ease;
}
.empty-gallery-scene {
  position: absolute;
  inset: 0 36% 0 0;
  perspective: 900px;
  opacity: 0.78;
}
.empty-gallery-shadow {
  position: absolute;
  left: 50%;
  bottom: 57px;
  width: 270px;
  height: 32px;
  border-radius: 50%;
  background: var(--empty-shadow);
  filter: blur(18px);
  transform: translateX(calc(-50% + var(--shift-x) * -0.3));
  transition: transform 240ms ease;
}
.empty-gallery-orbit {
  position: absolute;
  left: 50%;
  top: 51%;
  width: 270px;
  height: 270px;
  border: 1px solid color-mix(in srgb, var(--scene-accent) 26%, transparent);
  border-radius: 50%;
  transform: translate(-50%, -50%) translate(calc(var(--shift-x) * -0.22), calc(var(--shift-y) * -0.22));
  transition: transform 240ms ease;
}
.empty-gallery-paper {
  position: absolute;
  left: 50%;
  top: 50%;
  box-shadow: 0 17px 34px var(--empty-shadow);
  transition: transform 240ms cubic-bezier(0.2, 0.75, 0.25, 1);
}
.paper-back {
  width: 190px;
  height: 245px;
  background: var(--scene-soft);
  transform: translate(-63%, -53%) rotate(-9deg) scale(0.84)
    translate(calc(var(--shift-x) * -0.55), calc(var(--shift-y) * -0.55));
}
.paper-middle {
  width: 178px;
  height: 230px;
  padding: 24px;
  background: color-mix(in srgb, var(--scene-accent) 76%, var(--empty-surface));
  transform: translate(-35%, -48%) rotate(8deg) scale(0.84)
    translate(calc(var(--shift-x) * 0.3), calc(var(--shift-y) * 0.3));
}
.paper-middle span {
  display: block;
  width: 58%;
  height: 1px;
  margin-bottom: 10px;
  background: rgb(255 255 255 / 42%);
}
.paper-middle span:nth-child(2) {
  width: 32%;
}
.paper-middle span:nth-child(3) {
  width: 44%;
}
.paper-front {
  width: 205px;
  height: 260px;
  overflow: hidden;
  border: 4px solid var(--empty-paper-edge);
  background: var(--empty-paper);
  transform: translate(-50%, -52%) rotateX(var(--tilt-x)) rotateY(var(--tilt-y)) scale(0.84)
    translate(var(--shift-x), var(--shift-y));
}
.paper-rule {
  position: absolute;
  top: 25px;
  left: 24px;
  width: 46px;
  height: 2px;
  background: var(--scene-accent);
}
.paper-arch {
  position: absolute;
  left: 30px;
  bottom: 27px;
  width: 86px;
  height: 140px;
  border-radius: 48px 48px 0 0;
  background: var(--scene-accent);
}
.paper-circle {
  position: absolute;
  right: 25px;
  top: 67px;
  width: 74px;
  height: 74px;
  border-radius: 50%;
  background: var(--scene-soft);
  mix-blend-mode: multiply;
}
.paper-square {
  position: absolute;
  right: 25px;
  bottom: 27px;
  width: 58px;
  height: 58px;
  background: var(--scene-glow);
}
.empty-gallery-dot {
  position: absolute;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--scene-accent);
  transition: transform 260ms ease;
}
.dot-one {
  left: 22%;
  top: 28%;
  transform: translate(calc(var(--shift-x) * -0.9), calc(var(--shift-y) * -0.9));
}
.dot-two {
  right: 17%;
  bottom: 24%;
  width: 6px;
  height: 6px;
  transform: translate(calc(var(--shift-x) * 0.8), calc(var(--shift-y) * 0.8));
}
.empty-gallery-copy {
  position: absolute;
  top: 50%;
  right: 10%;
  min-width: 150px;
  transform: translateY(-50%);
}
.empty-gallery-copy h2 {
  margin: 0 0 28px;
  font-size: clamp(26px, 2.6vw, 38px);
  font-weight: 600;
  letter-spacing: -0.04em;
}
.empty-gallery-action {
  padding: 10px 0;
  border-bottom: 1px solid var(--ink);
  background: transparent;
  color: var(--ink);
  font-size: 14px;
}
.empty-gallery-action:hover,
.empty-gallery-action:focus-visible {
  color: var(--scene-accent);
  border-color: var(--scene-accent);
}
@media (max-width: 700px) {
  .empty-gallery {
    min-height: 490px;
  }
  .empty-gallery-scene {
    inset: 0 0 120px;
    transform: scale(0.82);
  }
  .empty-gallery-copy {
    top: auto;
    right: 0;
    bottom: 42px;
    left: 0;
    text-align: center;
    transform: none;
  }
  .empty-gallery-copy h2 {
    margin-bottom: 15px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .empty-gallery *,
  .empty-gallery *:before,
  .empty-gallery *:after {
    transition: none !important;
  }
}
</style>
