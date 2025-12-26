import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080'

export const getMedicalOCRData = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/AA/A0/AAA0_0100`)
    return response.data
  } catch (error) {
    console.error('Error fetching medical OCR data:', error)
    throw error
  }
}

export const processMedicalDocument = async (data: any) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/AA/A0/AAA0_0100/process`, data)
    return response.data
  } catch (error) {
    console.error('Error processing medical document:', error)
    throw error
  }
}