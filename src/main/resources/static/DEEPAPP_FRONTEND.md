# DeepApp Frontend Development Guide

## Overview

This `frontend` directory contains the frontend source code for the DeepApp dashboard application. It was copied from the template `remix-of-swift-insight-dashboard` to preserve the original template for future updates.

## Directory Structure

```
/root/deepapp/deepapp_main/src/main/resources/static/
├── remix-of-swift-insight-dashboard/    # Original template (DO NOT MODIFY)
├── frontend/                           # Active development source (MODIFY HERE)
│   ├── src/                           # Source code
│   ├── public/                        # Static assets
│   ├── package.json                   # Dependencies
│   ├── vite.config.ts                 # Vite configuration
│   ├── tailwind.config.ts             # Tailwind CSS config
│   └── README.md                      # Original template README
└── DEEPAPP_FRONTEND.md               # This file
```

## Purpose

- **Template Preservation**: `remix-of-swift-insight-dashboard` remains untouched for template updates
- **Development Isolation**: All frontend development happens in `frontend/`
- **Clean Separation**: Frontend UI/UX development separate from backend logic

## Development Setup

### Prerequisites
- Node.js (v16 or higher)
- npm or bun package manager

### Installation

```bash
# Navigate to frontend directory
cd /root/deepapp/deepapp_main/src/main/resources/static/frontend

# Install dependencies
npm install
# or if using bun:
# bun install
```

### Development Server

```bash
# Start development server
npm run dev
# or
bun run dev
```

The development server will start on `http://localhost:5173` (default Vite port).

### Build for Production

```bash
# Build for production
npm run build
# or
bun run build
```

Built files will be in the `dist/` directory and served by Spring Boot.

## Integration with DeepApp Backend

### API Endpoints
- Backend API base URL: `http://localhost:8080` (when Spring Boot is running)
- OCR endpoints: `/api/ocr/*`
- Document management: `/api/documents/*`
- YOLO detection: `/api/detect/*`

### Static File Serving
- Spring Boot serves static files from `/src/main/resources/static/`
- Built frontend files should be in `/src/main/resources/static/frontend/dist/`
- Access frontend at: `http://localhost:8080/` (root path)

## Development Workflow

1. **Make changes** in `frontend/src/`
2. **Test locally** with `npm run dev`
3. **Build for production** with `npm run build`
4. **Spring Boot** will automatically serve the built files

## File Organization

```
frontend/
├── src/
│   ├── components/     # Reusable UI components
│   ├── pages/         # Page components
│   ├── hooks/         # Custom React hooks
│   ├── utils/         # Utility functions
│   ├── types/         # TypeScript type definitions
│   ├── App.tsx        # Main app component
│   └── main.tsx       # App entry point
├── public/            # Static assets (images, icons, etc.)
└── dist/             # Built files (generated)
```

## Technologies Used

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **Tailwind CSS** - Utility-first CSS framework
- **shadcn/ui** - Component library
- **Lucide React** - Icons

## Notes

- **Do not modify** `remix-of-swift-insight-dashboard` - it's the pristine template
- **All development** should happen in `frontend/`
- When template updates are released, manually compare and merge changes
- Backend API integration should use relative URLs or proxy configuration

## Troubleshooting

### Port Conflicts
If port 5173 is busy:
```bash
# Use different port
npm run dev -- --port 3000
```

### Build Issues
```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Spring Boot Integration
Ensure Spring Boot serves static files correctly by checking `application.properties` for static resource configuration.