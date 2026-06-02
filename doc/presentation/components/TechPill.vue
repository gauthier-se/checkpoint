<!-- Small tech/label chip with an optional icon. -->
<script setup lang="ts">
defineProps<{ icon?: string }>()
</script>

<template>
  <span class="cp-pill">
    <span v-if="icon" :class="icon" class="cp-pill__icon" />
    <slot />
  </span>
</template>

<style scoped>
.cp-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.32rem;
  border-radius: 9999px;
  border: 1px solid var(--cp-border);
  background: var(--cp-card-2);
  padding: 0.2rem 0.65rem;
  font-size: 0.74rem;
  line-height: 1.2;
  white-space: nowrap;
}
.cp-pill__icon {
  font-size: 0.92rem;
  color: oklch(0.7 0.14 286);
}
</style>
