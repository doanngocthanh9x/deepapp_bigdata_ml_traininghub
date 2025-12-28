# React + Spring Boot Integration Guide

## Overview
This project integrates a React frontend with a Spring Boot backend, serving the React app as static files directly from the Java application.

## Build Process

### Automatic Build (Recommended)
The frontend is automatically built during the Maven build process using the `frontend-maven-plugin`:

```bash
mvn clean compile
mvn spring-boot:run
```

### Manual Build
You can also build the frontend manually:

```bash
cd src/main/resources/fronends/swift-dashboard
npm run build:java
```

## Running the Application

### Development Mode
For development with hot reloading:
```bash
cd src/main/resources/fronends/swift-dashboard
npm run dev
```
Then start the Java backend separately:
```bash
mvn spring-boot:run
```

### Production Mode
For production deployment:
```bash
mvn clean compile
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

## Architecture

### Frontend Build Configuration
- **Location**: `src/main/resources/fronends/swift-dashboard/`
- **Build Output**: `src/main/resources/static/`
- **Framework**: React 18 + Vite + TypeScript
- **UI Library**: ShadCN UI + Tailwind CSS

### Backend Configuration
- **Controller**: `WebController.java` handles SPA routing
- **Static Files**: Served from `src/main/resources/static/`
- **API Routes**: `/AA/*` routes are proxied to backend services
- **SPA Routing**: All non-API routes serve `index.html`

### Key Files
- `vite.config.ts`: Configures production build output to Java static directory
- `WebController.java`: Handles SPA routing with catch-all route
- `pom.xml`: Includes frontend-maven-plugin for automated builds

## API Integration

### Backend API Endpoints
- `/AA/A0/AAA0_0100/*` - Authentication services
- `/AA/A0/AAA0_0101/*` - OCR services
- `/AA/A0/AAA0_0200/*` - YOLO detection services
- `/ZZ/A0/ZZA0_0100/*` - Document processing services

### Frontend API Calls
The React app makes API calls to `/AA/*` routes, which are handled by the Java backend.

## Deployment

### Single Application Deployment
1. Build the entire application: `mvn clean package`
2. Run the JAR: `java -jar target/deepapp_main-0.0.1-SNAPSHOT.jar`
3. Access at `http://localhost:8080`

### Docker Deployment
The application can be containerized using the provided Dockerfile for complete deployment.

## Development Workflow

1. **Frontend Changes**: Modify files in `src/main/resources/fronends/swift-dashboard/src/`
2. **Backend Changes**: Modify Java files in `src/main/java/`
3. **Build & Test**: Run `mvn clean compile && mvn spring-boot:run`
4. **API Testing**: Use endpoints like `/AA/A0/AAA0_0101` for backend services

## Troubleshooting

### Build Issues
- Ensure Node.js v18+ is installed
- Clear node_modules: `rm -rf src/main/resources/fronends/swift-dashboard/node_modules && npm install`

### Runtime Issues
- Check that static files are generated in `src/main/resources/static/`
- Verify API routes are accessible
- Check browser console for frontend errors

### Port Conflicts
- Default port is 8080, can be changed in `application.properties`