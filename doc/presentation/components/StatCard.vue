<!-- Animated count-up stat card for the "chiffres clés" slide. -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'

const props = withDefaults(
  defineProps<{
    value: number
    label: string
    prefix?: string
    suffix?: string
    icon?: string
  }>(),
  { prefix: '', suffix: '' },
)

const display = ref(0)

onMounted(() => {
  const duration = 1100
  const start = performance.now()
  const tick = (now: number) => {
    const t = Math.min(1, (now - start) / duration)
    // easeOutCubic
    const eased = 1 - Math.pow(1 - t, 3)
    display.value = Math.round(props.value * eased)
    if (t < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
})
</script>

<template>
  <div class="cp-stat">
    <div v-if="icon" class="cp-stat__icon" :class="icon" />
    <div class="cp-stat__num">
      <span class="cp-stat__affix">{{ prefix }}</span>{{ display
      }}<span class="cp-stat__affix">{{ suffix }}</span>
    </div>
    <div class="cp-stat__label">{{ label }}</div>
  </div>
</template>

<style scoped>
.cp-stat {
  position: relative;
  text-align: center;
  border-radius: 14px;
  border: 1px solid var(--cp-border);
  background: linear-gradient(180deg, color-mix(in oklch, var(--cp-primary) 10%, var(--cp-card)), var(--cp-card));
  padding: 1rem 0.75rem 0.9rem;
}
.cp-stat__icon {
  font-size: 1.3rem;
  color: oklch(0.66 0.18 286);
  margin-bottom: 0.2rem;
}
.cp-stat__num {
  font-family: var(--slidev-code-font-family, 'JetBrains Mono', monospace);
  font-size: 2.2rem;
  font-weight: 750;
  line-height: 1;
  background: linear-gradient(180deg, oklch(0.82 0.12 286), oklch(0.58 0.21 286));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-variant-numeric: tabular-nums;
}
.cp-stat__affix {
  font-size: 1.3rem;
}
.cp-stat__label {
  margin-top: 0.35rem;
  font-size: 0.72rem;
  color: var(--cp-foreground-muted);
  line-height: 1.25;
}
</style>
