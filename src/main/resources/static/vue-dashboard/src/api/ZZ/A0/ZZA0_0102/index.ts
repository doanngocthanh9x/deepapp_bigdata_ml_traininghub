import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080'

export const getYOLODetectionData = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/ZZ/A0/ZZA0_0102`)
    return response.data
  } catch (error) {
    console.error('Error fetching YOLO detection data:', error)
    throw error
  }
}

export const processYOLODetection = async (data: any) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/ZZ/A0/ZZA0_0102/process`, data)
    return response.data
  } catch (error) {
    console.error('Error processing YOLO detection:', error)
    throw error
  }
}