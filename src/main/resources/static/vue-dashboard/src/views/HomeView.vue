<template>
  <div class="max-w-7xl mx-auto p-6 space-y-6">
    <!-- Welcome Section -->
    <div class="bg-white rounded-lg shadow-sm p-6">
      <h1 class="text-3xl font-bold text-gray-900 mb-2">Welcome to DeepApp SaaS Dashboard</h1>
      <p class="text-gray-600">Monitor your AI-powered document processing services</p>
    </div>

    <!-- Metrics Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div class="bg-white rounded-lg shadow-sm p-6">
        <div class="flex items-center">
          <div class="p-2 bg-blue-100 rounded-lg">
            <i class="fas fa-file-medical text-blue-600 text-xl"></i>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-600">Medical OCR</p>
            <p class="text-2xl font-bold text-gray-900">{{ metrics.medicalProcessed }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow-sm p-6">
        <div class="flex items-center">
          <div class="p-2 bg-green-100 rounded-lg">
            <i class="fas fa-search text-green-600 text-xl"></i>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-600">YOLO Detections</p>
            <p class="text-2xl font-bold text-gray-900">{{ metrics.yoloDetections }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow-sm p-6">
        <div class="flex items-center">
          <div class="p-2 bg-yellow-100 rounded-lg">
            <i class="fas fa-clock text-yellow-600 text-xl"></i>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-600">Avg Response Time</p>
            <p class="text-2xl font-bold text-gray-900">{{ metrics.avgResponseTime }}s</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow-sm p-6">
        <div class="flex items-center">
          <div class="p-2 bg-purple-100 rounded-lg">
            <i class="fas fa-users text-purple-600 text-xl"></i>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-600">Active Users</p>
            <p class="text-2xl font-bold text-gray-900">{{ metrics.activeUsers }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Charts Section -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Usage Chart -->
      <div class="bg-white rounded-lg shadow-sm p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">Service Usage (Last 7 Days)</h3>
        <div class="h-64">
          <canvas ref="usageChart"></canvas>
        </div>
      </div>

      <!-- Performance Chart -->
      <div class="bg-white rounded-lg shadow-sm p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">Performance Metrics</h3>
        <div class="h-64">
          <canvas ref="performanceChart"></canvas>
        </div>
      </div>
    </div>

    <!-- Recent Activity -->
    <div class="bg-white rounded-lg shadow-sm p-6">
      <h3 class="text-lg font-semibold text-gray-900 mb-4">Recent Activity</h3>
      <div class="space-y-3">
        <div v-for="activity in recentActivities" :key="activity.id" class="flex items-center p-3 bg-gray-50 rounded-lg">
          <div class="flex-shrink-0">
            <i :class="activity.icon" :style="{ color: activity.color }" class="text-lg"></i>
          </div>
          <div class="ml-3 flex-1">
            <p class="text-sm font-medium text-gray-900">{{ activity.message }}</p>
            <p class="text-xs text-gray-500">{{ activity.time }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, BarElement, LineController, BarController } from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, LineController, BarController)

const metrics = ref({
  medicalProcessed: 1247,
  yoloDetections: 892,
  avgResponseTime: 2.3,
  activeUsers: 156
})

const recentActivities = ref([
  {
    id: 1,
    message: 'Medical document processed successfully',
    time: '2 minutes ago',
    icon: 'fas fa-check-circle',
    color: '#10B981'
  },
  {
    id: 2,
    message: 'YOLO detection completed for batch #1234',
    time: '5 minutes ago',
    icon: 'fas fa-search',
    color: '#3B82F6'
  },
  {
    id: 3,
    message: 'New user registered',
    time: '10 minutes ago',
    icon: 'fas fa-user-plus',
    color: '#8B5CF6'
  },
  {
    id: 4,
    message: 'System maintenance completed',
    time: '1 hour ago',
    icon: 'fas fa-cog',
    color: '#F59E0B'
  }
])

const usageChart = ref(null)
const performanceChart = ref(null)

onMounted(() => {
  // Initialize charts
  if (usageChart.value) {
    new ChartJS(usageChart.value, {
      type: 'line',
      data: {
        labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
        datasets: [{
          label: 'Medical OCR',
          data: [120, 150, 180, 200, 170, 140, 160],
          borderColor: '#3B82F6',
          backgroundColor: '#3B82F640',
          tension: 0.4
        }, {
          label: 'YOLO Detection',
          data: [80, 100, 120, 140, 110, 90, 100],
          borderColor: '#10B981',
          backgroundColor: '#10B98140',
          tension: 0.4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top' as const,
          }
        }
      }
    })
  }

  if (performanceChart.value) {
    new ChartJS(performanceChart.value, {
      type: 'bar',
      data: {
        labels: ['Accuracy', 'Speed', 'Reliability'],
        datasets: [{
          label: 'Performance Score',
          data: [95, 88, 92],
          backgroundColor: ['#3B82F6', '#10B981', '#F59E0B']
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            max: 100
          }
        }
      }
    })
  }
})
</script>

<style scoped>
/* Additional custom styles if needed */
</style>
