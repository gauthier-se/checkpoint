<!-- Persistent footer rendered on every slide (hidden on the cover). -->
<script setup lang="ts">
import { useNav } from '@slidev/client'
const { currentPage, total } = useNav()
</script>

<template>
  <footer v-if="currentPage > 1" class="cp-footer">
    <span class="flex items-center gap-2">
      <img src="/logo.svg" alt="CheckPoint" />
      <span>CheckPoint</span>
    </span>
    <span class="cp-pagenum">{{ currentPage }} / {{ total }}</span>
  </footer>
</template>
