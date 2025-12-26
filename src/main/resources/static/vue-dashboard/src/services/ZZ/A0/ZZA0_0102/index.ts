import { getYOLODetectionData, processYOLODetection } from '../../api/ZZ/A0/ZZA0_0102'

export class YOLODetectionService {
  static async fetchData() {
    return await getYOLODetectionData()
  }

  static async processImage(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return await processYOLODetection(formData)
  }

  static validateImage(file: File): boolean {
    // Business logic for validation
    const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg']
    const maxSize = 20 * 1024 * 1024 // 20MB

    return allowedTypes.includes(file.type) && file.size <= maxSize
  }
}