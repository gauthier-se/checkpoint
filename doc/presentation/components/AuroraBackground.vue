<!--
  Subtle violet ambience for cover / section slides.
  No grid, no heavy gradient — just two soft, low-opacity glows.
-->
<template>
  <div class="cp-aurora" aria-hidden="true">
    <div class="cp-aurora__glow cp-aurora__glow--1" />
    <div class="cp-aurora__glow cp-aurora__glow--2" />
  </div>
</template>

<style scoped>
.cp-aurora {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}
.cp-aurora__glow {
  position: absolute;
  border-radius: 9999px;
  filter: blur(90px);
}
.cp-aurora__glow--1 {
  width: 40vw;
  height: 40vw;
  top: -16%;
  left: -8%;
  background: radial-gradient(circle, oklch(0.5 0.18 286 / 0.28), transparent 68%);
  animation: cp-drift-1 22s ease-in-out infinite alternate;
}
.cp-aurora__glow--2 {
  width: 32vw;
  height: 32vw;
  bottom: -18%;
  right: -6%;
  background: radial-gradient(circle, oklch(0.46 0.16 300 / 0.22), transparent 68%);
  animation: cp-drift-2 26s ease-in-out infinite alternate;
}
@keyframes cp-drift-1 {
  to {
    transform: translate(4%, 6%) scale(1.08);
  }
}
@keyframes cp-drift-2 {
  to {
    transform: translate(-5%, -4%) scale(1.1);
  }
}
</style>
