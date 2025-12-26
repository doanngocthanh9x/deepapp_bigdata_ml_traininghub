# Vue Dashboard for DeepApp

A modern, lightweight Vue.js dashboard for the DeepApp document processing system.

## Features

- **Modular Architecture**: Organized by backend modules (AA/A0/AAA0_0100, ZZ/A0/ZZA0_0102, etc.)
- **Real-time Statistics**: Live system metrics and processing statistics
- **Responsive Design**: Works on desktop and mobile devices
- **Modern UI**: Clean, professional interface with Bootstrap styling
- **Fast Development**: Built with Vite for rapid development and hot reloading

## Project Structure

```
src/
├── views/
│   ├── AA/A0/AAA0_0100/
│   │   └── AAA0_0100.vue          # Medical Document OCR Module
│   ├── ZZ/A0/ZZA0_0102/
│   │   └── ZZA0_0102.vue          # YOLO Object Detection Module
│   ├── HomeView.vue               # Main Dashboard
│   └── AboutView.vue              # About Page
├── router/
│   └── index.ts                   # Vue Router configuration
├── components/                    # Reusable components
├── stores/                        # Pinia state management
└── App.vue                        # Root component
```

## Getting Started

### Prerequisites

- Node.js v20.19.0 or higher
- npm v10.8.2 or higher

### Installation

1. **Install Node Version Manager (nvm)** (if not already installed):
   ```bash
   curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
   ```

2. **Load nvm and install Node.js v20**:
   ```bash
   export NVM_DIR="$HOME/.nvm"
   [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
   nvm install 20
   nvm use 20
   ```

3. **Install dependencies**:
   ```bash
   npm install
   ```

4. **Start development server**:
   ```bash
   npm run dev
   ```

5. **Open your browser** and navigate to `http://localhost:5173` (or the port shown in terminal)

## Available Routes

- `/` - Main Dashboard with system overview
- `/AA/A0/AAA0_0100` - Medical Document OCR Module
- `/ZZ/A0/ZZA0_0102` - YOLO Object Detection Module
- `/about` - About page

## Adding New Modules

To add a new processing module:

1. **Create the directory structure**:
   ```bash
   mkdir -p src/views/XX/A0/XXX0_0000
   ```

2. **Create the Vue component**:
   ```vue
   <!-- src/views/XX/A0/XXX0_0000/XXX0_0000.vue -->
   <template>
     <div class="module-container">
       <!-- Your module content here -->
     </div>
   </template>

   <script setup lang="ts">
   // Your module logic here
   </script>

   <style scoped>
   /* Your module styles here */
   </style>
   ```

3. **Add the route** in `src/router/index.ts`:
   ```typescript
   {
     path: '/XX/A0/XXX0_0000',
     name: 'XXX0_0000',
     component: () => import('../views/XX/A0/XXX0_0000/XXX0_0000.vue'),
   },
   ```

4. **Add navigation** in `src/views/HomeView.vue`:
   ```vue
   <div class="module-card" @click="navigateToModule('XXX0_0000')">
     <!-- Module card content -->
   </div>
   ```

## API Integration

The dashboard is designed to work with the Spring Boot backend. Add API calls using Axios:

```typescript
import axios from 'axios'

const fetchData = async () => {
  try {
    const response = await axios.get('/api/endpoint')
    // Handle response
  } catch (error) {
    console.error('API Error:', error)
  }
}
```

## Building for Production

```bash
npm run build
```

The built files will be in the `dist/` directory, ready for deployment.

## Technologies Used

- **Vue 3**: Progressive JavaScript framework
- **TypeScript**: Type-safe JavaScript
- **Vite**: Fast build tool and development server
- **Vue Router**: Official router for Vue.js
- **Bootstrap 5**: CSS framework for responsive design
- **Axios**: HTTP client for API calls
- **Chart.js**: Charts and graphs (available for future use)

## Development Notes

- Hot reloading is enabled during development
- ESLint is configured for code quality
- TypeScript provides type checking
- The project follows Vue 3 Composition API patterns

## Troubleshooting

### Port already in use
If port 5173 is busy, Vite will automatically use the next available port (5174, 5175, etc.).

### Node.js version issues
Make sure you're using Node.js v20+:
```bash
node --version  # Should show v20.x.x
npm --version   # Should show 10.x.x
```

### Dependencies issues
Clear node_modules and reinstall:
```bash
rm -rf node_modules package-lock.json
npm install
```
