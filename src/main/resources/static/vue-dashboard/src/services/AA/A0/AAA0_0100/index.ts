import { getMedicalOCRData, processMedicalDocument } from '../../api/AA/A0/AAA0_0100'

export class MedicalOCRService {
  static async fetchData() {
    return await getMedicalOCRData()
  }

  static async processDocument(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return await processMedicalDocument(formData)
  }

  static validateDocument(file: File): boolean {
    // Business logic for validation
    const allowedTypes = ['image/jpeg', 'image/png', 'application/pdf']
    const maxSize = 10 * 1024 * 1024 // 10MB

    return allowedTypes.includes(file.type) && file.size <= maxSize
  }
}