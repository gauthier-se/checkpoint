<!--
  Section divider layout. Frontmatter:
    layout: section
    number: "02"
-->
<script setup lang="ts">
defineProps<{ number?: string }>()
</script>

<template>
  <div class="slidev-layout section cp-section">
    <AuroraBackground />
    <div class="cp-section__inner">
      <div v-if="number" class="cp-section__num">{{ number }}</div>
      <div class="cp-section__content">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>
.cp-section {
  position: relative;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 3rem 4.5rem;
}
.cp-section__inner {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 1.5rem;
}
.cp-section__num {
  font-family: var(--slidev-code-font-family, 'JetBrains Mono', monospace);
  font-size: 7rem;
  font-weight: 800;
  line-height: 0.9;
  background: linear-gradient(160deg, oklch(0.62 0.21 286), oklch(0.4 0.18 286) 70%, transparent);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  opacity: 0.9;
}
.cp-section__content :deep(h1) {
  font-size: 2.6rem;
  margin: 0;
}
.cp-section__content :deep(p) {
  color: var(--cp-foreground-muted);
  margin-top: 0.4rem;
  font-size: 1rem;
}
</style>
