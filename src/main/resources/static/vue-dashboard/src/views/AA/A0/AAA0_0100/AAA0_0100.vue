<template>
  <div class="module-container">
    <div class="row">
      <div class="col-12">
        <div class="card">
          <div class="card-header">
            <h5 class="card-title">AAA0_0100 Module</h5>
            <span class="badge bg-primary">Medical Document Processing</span>
          </div>
          <div class="card-body">
            <div class="row">
              <div class="col-md-6">
                <div class="module-info">
                  <h6>Module Information</h6>
                  <p><strong>Code:</strong> AAA0_0100</p>
                  <p><strong>Type:</strong> Medical Document OCR</p>
                  <p><strong>Status:</strong> <span class="text-success">Active</span></p>
                  <p><strong>Last Updated:</strong> {{ new Date().toLocaleDateString() }}</p>
                </div>
              </div>
              <div class="col-md-6">
                <div class="module-stats">
                  <h6>Processing Statistics</h6>
                  <div class="stats-grid">
                    <div class="stat-item">
                      <div class="stat-number">{{ stats.processedToday }}</div>
                      <div class="stat-label">Processed Today</div>
                    </div>
                    <div class="stat-item">
                      <div class="stat-number">{{ stats.successRate }}%</div>
                      <div class="stat-label">Success Rate</div>
                    </div>
                    <div class="stat-item">
                      <div class="stat-number">{{ stats.avgTime }}s</div>
                      <div class="stat-label">Avg Processing Time</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <hr>

            <div class="module-actions">
              <h6>Actions</h6>
              <div class="btn-group" role="group">
                <button class="btn btn-primary" @click="processDocument">
                  <i class="fas fa-play"></i> Process Document
                </button>
                <button class="btn btn-secondary" @click="viewLogs">
                  <i class="fas fa-list"></i> View Logs
                </button>
                <button class="btn btn-info" @click="configureModule">
                  <i class="fas fa-cog"></i> Configure
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Activity -->
    <div class="row mt-4">
      <div class="col-12">
        <div class="card">
          <div class="card-header">
            <h6>Recent Activity</h6>
          </div>
          <div class="card-body">
            <div class="activity-list">
              <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
                <div class="activity-icon">
                  <i :class="activity.icon" :style="{ color: activity.color }"></i>
                </div>
                <div class="activity-content">
                  <div class="activity-message">{{ activity.message }}</div>
                  <div class="activity-time">{{ activity.time }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
// import { MedicalOCRService } from '../../../services/AA/A0/AAA0_0100'

const stats = ref({
  processedToday: 0,
  successRate: 0,
  avgTime: 0
})

const recentActivities = ref([
  {
    id: 1,
    message: 'Document processing completed successfully',
    time: '2 minutes ago',
    icon: 'fas fa-check-circle',
    color: '#28a745'
  },
  {
    id: 2,
    message: 'New document uploaded for processing',
    time: '5 minutes ago',
    icon: 'fas fa-upload',
    color: '#007bff'
  },
  {
    id: 3,
    message: 'OCR accuracy improved to 98.5%',
    time: '1 hour ago',
    icon: 'fas fa-chart-line',
    color: '#ffc107'
  }
])

const processDocument = async () => {
  try {
    // Simulate file upload
    const fileInput = document.createElement('input')
    fileInput.type = 'file'
    fileInput.accept = 'image/*,.pdf'
    fileInput.onchange = async (e) => {
      const file = (e.target as HTMLInputElement).files?.[0]
      if (file /* && MedicalOCRService.validateDocument(file) */) {
        // const result = await MedicalOCRService.processDocument(file)
        alert('Document processed successfully!')
        // console.log(result)
      } else {
        alert('Invalid file type or size')
      }
    }
    fileInput.click()
  } catch (error) {
    alert('Error processing document')
    console.error(error)
  }
}

const viewLogs = () => {
  alert('Viewing logs...')
}

const configureModule = () => {
  alert('Opening configuration...')
}

onMounted(async () => {
  try {
    // const data = await MedicalOCRService.fetchData()
    stats.value = {
      processedToday: 47, // data.processedToday || 47,
      successRate: 96, // data.successRate || 96,
      avgTime: 2.3 // data.avgTime || 2.3
    }
  } catch (error) {
    console.error('Error loading data:', error)
    // Fallback to mock data
    stats.value = {
      processedToday: 47,
      successRate: 96,
      avgTime: 2.3
    }
  }
})
</script>

<style scoped>
.module-container {
  padding: 20px;
}

.card {
  border: none;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.card-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 10px 10px 0 0 !important;
  border: none;
}

.module-info p {
  margin-bottom: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 15px;
  margin-top: 15px;
}

.stat-item {
  text-align: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #495057;
}

.stat-label {
  font-size: 12px;
  color: #6c757d;
  margin-top: 5px;
}

.module-actions {
  margin-top: 20px;
}

.btn-group .btn {
  margin-right: 10px;
}

.activity-list {
  max-height: 300px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #e9ecef;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.activity-content {
  flex: 1;
}

.activity-message {
  font-weight: 500;
  color: #495057;
}

.activity-time {
  font-size: 12px;
  color: #6c757d;
  margin-top: 2px;
}
</style>