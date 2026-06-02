<!-- Cover layout: aurora background + centered content slot. -->
<template>
  <div class="slidev-layout cover cp-cover">
    <AuroraBackground />
    <div class="cp-cover__content">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.cp-cover {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 3rem 4rem;
}
.cp-cover__content {
  position: relative;
  z-index: 2;
  width: 100%;
}
</style>
