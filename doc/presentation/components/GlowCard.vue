<!-- Card with a soft violet glow + optional icon header. -->
<script setup lang="ts">
defineProps<{
  icon?: string
  title?: string
  color?: string
}>()
</script>

<template>
  <div class="cp-glow-card" :style="color ? { '--glow': color } : {}">
    <div v-if="icon || title" class="cp-glow-card__head">
      <div v-if="icon" class="cp-glow-card__icon" :class="icon" />
      <span v-if="title" class="cp-glow-card__title">{{ title }}</span>
    </div>
    <div class="cp-glow-card__body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.cp-glow-card {
  --glow: oklch(0.5775 0.2192 286);
  position: relative;
  border-radius: 14px;
  border: 1px solid var(--cp-border);
  background:
    linear-gradient(180deg, color-mix(in oklch, var(--glow) 8%, var(--cp-card)), var(--cp-card));
  padding: 0.85rem 1rem;
  overflow: hidden;
  height: 100%;
}
.cp-glow-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  padding: 1px;
  background: linear-gradient(140deg, color-mix(in oklch, var(--glow) 60%, transparent), transparent 45%);
  -webkit-mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}
.cp-glow-card__head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.4rem;
}
.cp-glow-card__icon {
  font-size: 1.15rem;
  color: var(--glow);
}
.cp-glow-card__title {
  font-weight: 650;
  font-size: 0.98rem;
}
.cp-glow-card__body {
  font-size: 0.82rem;
  line-height: 1.45;
  color: var(--cp-foreground);
}
.cp-glow-card__body :deep(p) {
  margin: 0.2em 0;
}
.cp-glow-card__body :deep(strong) {
  color: color-mix(in oklch, var(--glow) 55%, var(--cp-foreground));
}
</style>
