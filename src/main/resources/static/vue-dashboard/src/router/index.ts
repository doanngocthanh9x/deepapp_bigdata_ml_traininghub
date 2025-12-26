import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
    // Auto-generated routes for modules
    {
      path: '/AA/A0/AAA0_0100',
      name: 'AAA0_0100',
      component: () => import('../views/AA/A0/AAA0_0100/AAA0_0100.vue'),
    },
    {
      path: '/ZZ/A0/ZZA0_0102',
      name: 'ZZA0_0102',
      component: () => import('../views/ZZ/A0/ZZA0_0102/ZZA0_0102.vue'),
    },
  ],
})

export default router
