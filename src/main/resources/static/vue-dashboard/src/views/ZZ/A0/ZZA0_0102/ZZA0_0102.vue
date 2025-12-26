<template>
  <div class="module-container">
    <div class="row">
      <div class="col-12">
        <div class="card">
          <div class="card-header">
            <h5 class="card-title">ZZA0_0102 Module</h5>
            <span class="badge bg-success">YOLO Object Detection</span>
          </div>
          <div class="card-body">
            <div class="row">
              <div class="col-md-6">
                <div class="module-info">
                  <h6>Module Information</h6>
                  <p><strong>Code:</strong> ZZA0_0102</p>
                  <p><strong>Type:</strong> YOLO Object Detection</p>
                  <p><strong>Status:</strong> <span class="text-success">Active</span></p>
                  <p><strong>Model:</strong> YOLOv8</p>
                  <p><strong>Last Updated:</strong> {{ new Date().toLocaleDateString() }}</p>
                </div>
              </div>
              <div class="col-md-6">
                <div class="module-stats">
                  <h6>Detection Statistics</h6>
                  <div class="stats-grid">
                    <div class="stat-item">
                      <div class="stat-number">{{ stats.objectsDetected }}</div>
                      <div class="stat-label">Objects Detected</div>
                    </div>
                    <div class="stat-item">
                      <div class="stat-number">{{ stats.accuracy }}%</div>
                      <div class="stat-label">Detection Accuracy</div>
                    </div>
                    <div class="stat-item">
                      <div class="stat-number">{{ stats.avgTime }}ms</div>
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
                <button class="btn btn-success" @click="runDetection">
                  <i class="fas fa-search"></i> Run Detection
                </button>
                <button class="btn btn-secondary" @click="viewResults">
                  <i class="fas fa-eye"></i> View Results
                </button>
                <button class="btn btn-warning" @click="trainModel">
                  <i class="fas fa-graduation-cap"></i> Train Model
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Detection Results -->
    <div class="row mt-4">
      <div class="col-12">
        <div class="card">
          <div class="card-header">
            <h6>Recent Detections</h6>
          </div>
          <div class="card-body">
            <div class="detection-results">
              <div v-for="detection in recentDetections" :key="detection.id" class="detection-item">
                <div class="detection-image">
                  <div class="file-icon bg-primary text-white">
                    <i class="fas fa-file-image"></i>
                  </div>
                </div>
                <div class="detection-info">
                  <div class="detection-filename">{{ detection.filename }}</div>
                  <div class="detection-objects">
                    <span v-for="obj in detection.objects" :key="obj.class" class="object-tag">
                      {{ obj.class }} ({{ obj.confidence }}%)
                    </span>
                  </div>
                  <div class="detection-time">{{ detection.time }}</div>
                </div>
                <div class="detection-actions">
                  <button class="btn btn-sm btn-outline-primary" @click="viewFullImage(detection)">
                    <i class="fas fa-expand"></i>
                  </button>
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
// import { YOLODetectionService } from '../../../services/ZZ/A0/ZZA0_0102'

const stats = ref({
  objectsDetected: 0,
  accuracy: 0,
  avgTime: 0
})

const recentDetections = ref([
  {
    id: 1,
    filename: 'document_001.jpg',
    objects: [
      { class: 'text', confidence: 95 },
      { class: 'signature', confidence: 87 }
    ],
    time: '3 minutes ago'
  },
  {
    id: 2,
    filename: 'medical_form.jpg',
    objects: [
      { class: 'form', confidence: 92 },
      { class: 'stamp', confidence: 78 }
    ],
    time: '8 minutes ago'
  },
  {
    id: 3,
    filename: 'invoice_scan.png',
    objects: [
      { class: 'table', confidence: 89 },
      { class: 'logo', confidence: 94 }
    ],
    time: '15 minutes ago'
  }
])

const runDetection = async () => {
  try {
    const fileInput = document.createElement('input')
    fileInput.type = 'file'
    fileInput.accept = 'image/*'
    fileInput.onchange = async (e) => {
      const file = (e.target as HTMLInputElement).files?.[0]
      
    }
    fileInput.click()
  } catch (error) {
    alert('Error running detection')
    console.error(error)
  }
}

const viewResults = () => {
  alert('Viewing detection results...')
}

const trainModel = () => {
  alert('Training model...')
}

const viewFullImage = (detection: any) => {
  alert(`Viewing full image: ${detection.filename}`)
}

onMounted(async () => {
 
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
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
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

.detection-results {
  max-height: 400px;
  overflow-y: auto;
}

.detection-item {
  display: flex;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #e9ecef;
}

.detection-item:last-child {
  border-bottom: none;
}

.detection-image {
  margin-right: 15px;
}

.detection-image img {
  width: 80px;
  height: 60px;
  object-fit: cover;
}

.detection-info {
  flex: 1;
}

.detection-filename {
  font-weight: 500;
  color: #495057;
  margin-bottom: 5px;
}

.detection-objects {
  margin-bottom: 5px;
}

.object-tag {
  display: inline-block;
  background: #e9ecef;
  color: #495057;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  margin-right: 5px;
  margin-bottom: 3px;
}

.detection-time {
  font-size: 12px;
  color: #6c757d;
}

.detection-actions {
  margin-left: 15px;
}

.file-icon {
  width: 60px;
  height: 45px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 24px;
}
</style>