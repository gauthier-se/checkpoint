<!--
  Screenshot placeholder. Drop a real image at /screenshots/<src> and pass
  `src` to render it; otherwise a labelled dashed frame is shown.
  Usage: <Placeholder label="Démo web — recherche floue" src="web-search.png" />
-->
<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  label?: string
  src?: string
  ratio?: string
}>()

// Fall back to the dashed frame if the image is missing / fails to load.
const failed = ref(false)
</script>

<template>
  <div
    class="cp-ph"
    :style="{ aspectRatio: ratio || '16 / 9' }"
  >
    <img
      v-if="src && !failed"
      :src="`/screenshots/${src}`"
      :alt="label"
      class="cp-ph__img"
      @error="failed = true"
    />
    <div v-else class="cp-ph__empty">
      <div class="cp-ph__icon i-carbon-image" />
      <div class="cp-ph__label">{{ label || 'Capture d’écran' }}</div>
      <div class="cp-ph__hint">placeholder — à remplacer</div>
    </div>
  </div>
</template>

<style scoped>
.cp-ph {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--cp-border);
  background: var(--cp-card);
}
.cp-ph__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.cp-ph__empty {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  border: 1.5px dashed color-mix(in oklch, var(--cp-primary) 45%, var(--cp-border));
  border-radius: 12px;
  background:
    repeating-linear-gradient(
      45deg,
      transparent,
      transparent 12px,
      color-mix(in oklch, var(--cp-primary) 6%, transparent) 12px,
      color-mix(in oklch, var(--cp-primary) 6%, transparent) 24px
    );
}
.cp-ph__icon {
  font-size: 1.8rem;
  color: oklch(0.62 0.16 286);
  opacity: 0.85;
}
.cp-ph__label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--cp-foreground);
}
.cp-ph__hint {
  font-size: 0.65rem;
  color: var(--cp-foreground-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
</style>
