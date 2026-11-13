import { onBeforeUnmount, onMounted, ref } from 'vue'

export function useTelaCompacta() {
  const compacta = ref(false)
  const toque = window.matchMedia('(hover: none) and (pointer: coarse)')
  function atualizar() {
    compacta.value =
      getComputedStyle(document.documentElement).getPropertyValue('--layout-compacto').trim() ===
      '1'
  }
  onMounted(() => {
    atualizar()
    window.addEventListener('resize', atualizar)
    toque.addEventListener('change', atualizar)
  })
  onBeforeUnmount(() => {
    window.removeEventListener('resize', atualizar)
    toque.removeEventListener('change', atualizar)
  })
  return compacta
}
