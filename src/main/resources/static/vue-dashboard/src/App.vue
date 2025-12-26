<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 flex" :class="{ 'sidebar-open': sidebarOpen }">
    <!-- Sidebar -->
    <aside :class="['fixed inset-y-0 left-0 z-50 w-64 bg-white dark:bg-gray-800 shadow-lg transform transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:inset-0',
                   sidebarOpen ? 'translate-x-0' : '-translate-x-full']">
      <div class="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
        <h3 class="text-xl font-bold text-gray-900 dark:text-white flex items-center">
          <i class="fas fa-brain mr-3 text-blue-500"></i>
          DeepApp SaaS
        </h3>
        <button @click="sidebarOpen = false" class="lg:hidden p-2 rounded-md text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <nav class="mt-6 px-4">
        <router-link to="/" class="flex items-center px-4 py-3 mb-2 text-gray-700 dark:text-gray-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 hover:text-blue-600 dark:hover:text-blue-400 rounded-lg transition-colors"
                     active-class="bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300">
          <i class="fas fa-tachometer-alt mr-3 w-5 text-center"></i>
          <span class="font-medium">Dashboard</span>
        </router-link>
        <router-link to="/AA/A0/AAA0_0100" class="flex items-center px-4 py-3 mb-2 text-gray-700 dark:text-gray-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 hover:text-blue-600 dark:hover:text-blue-400 rounded-lg transition-colors"
                     active-class="bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300">
          <i class="fas fa-file-medical mr-3 w-5 text-center"></i>
          <span class="font-medium">Medical OCR</span>
        </router-link>
        <router-link to="/ZZ/A0/ZZA0_0102" class="flex items-center px-4 py-3 mb-2 text-gray-700 dark:text-gray-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 hover:text-blue-600 dark:hover:text-blue-400 rounded-lg transition-colors"
                     active-class="bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300">
          <i class="fas fa-search mr-3 w-5 text-center"></i>
          <span class="font-medium">YOLO Detection</span>
        </router-link>
        <a href="#" class="flex items-center px-4 py-3 mb-2 text-gray-700 dark:text-gray-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 hover:text-blue-600 dark:hover:text-blue-400 rounded-lg transition-colors">
          <i class="fas fa-cog mr-3 w-5 text-center"></i>
          <span class="font-medium">Settings</span>
        </a>
      </nav>
    </aside>

    <!-- Overlay for mobile -->
    <div v-if="sidebarOpen" @click="sidebarOpen = false" class="fixed inset-0 z-40 bg-black bg-opacity-50 lg:hidden"></div>

    <!-- Main Content -->
    <div class="flex-1 flex flex-col min-w-0 lg:ml-64">
      <!-- Top Header -->
      <header class="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700 sticky top-0 z-30">
        <div class="px-4 sm:px-6 py-4 flex items-center justify-between">
          <div class="flex items-center">
            <button @click="sidebarOpen = true" class="lg:hidden mr-4 p-2 rounded-md text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700">
              <i class="fas fa-bars"></i>
            </button>
            <div>
              <h1 class="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">Dashboard</h1>
              <p class="text-sm text-gray-600 dark:text-gray-400">Home / Dashboard</p>
            </div>
          </div>
          <div class="flex items-center space-x-4">
            <!-- Dark Mode Toggle -->
            <button @click="toggleDarkMode" class="p-2 rounded-md text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700">
              <i :class="darkMode ? 'fas fa-sun' : 'fas fa-moon'"></i>
            </button>
            <!-- Notifications -->
            <button class="p-2 rounded-md text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 relative">
              <i class="fas fa-bell"></i>
              <span class="absolute -top-1 -right-1 h-4 w-4 bg-red-500 text-white text-xs rounded-full flex items-center justify-center">3</span>
            </button>
            <!-- User Menu -->
            <div class="flex items-center text-gray-700 dark:text-gray-300">
              <i class="fas fa-user-circle text-2xl mr-2"></i>
              <span class="font-medium hidden sm:block">Admin</span>
            </div>
          </div>
        </div>
      </header>

      <!-- Page Content -->
      <main class="flex-1 overflow-auto">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

const sidebarOpen = ref(false)
const darkMode = ref(false)

const toggleDarkMode = () => {
  darkMode.value = !darkMode.value
  if (darkMode.value) {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
  localStorage.setItem('darkMode', darkMode.value.toString())
}

onMounted(() => {
  const savedDarkMode = localStorage.getItem('darkMode')
  if (savedDarkMode === 'true') {
    darkMode.value = true
    document.documentElement.classList.add('dark')
  }
})
</script>

<style scoped>
/* Additional styles if needed */
</style>
