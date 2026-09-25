<script setup lang="ts">
defineEmits<{ publish: [] }>()
</script>

<template>
  <section id="creators" class="creator-section content-width" aria-labelledby="creator-title">
    <div class="creator-image">
      <img
        src="/images/creator-studio-1280.webp"
        srcset="/images/creator-studio-640.webp 640w, /images/creator-studio-1280.webp 1280w"
        sizes="(max-width: 800px) 90vw, 52vw"
        width="1280"
        height="801"
        loading="lazy"
        decoding="async"
        alt="阳光下的创作桌面，摆着抽象设计、植物手稿和色卡"
      />
    </div>
    <div class="creator-copy">
      <h2 id="creator-title">发布作品</h2>
      <p>创建店铺，上传作品</p>
      <button class="button button-primary" type="button" @click="$emit('publish')">
        开始发布 <span aria-hidden="true">↗</span>
      </button>
    </div>
  </section>
</template>
