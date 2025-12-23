# DeepApp Main - Docker Deployment

## Overview
Scalable Java Spring Boot + C++ gRPC Workers architecture in Docker containers.

## Architecture
- **Java Layer**: Spring Boot 4.0.1 + gRPC clients
- **C++ Workers**: Auto-registered workers with gRPC communication
- **3-Layer Design**: Infrastructure → Workers → Modules

## Quick Start

### 1. Test Configuration
```bash
./docker-test.sh
```

### 2. Build Docker Image
```bash
./docker-build.sh
```

This will:
- Build C++ workers (multi-stage)
- Compile Java application
- Create optimized runtime image

### 3. Run Container
```bash
./docker-run.sh
```

The application will:
- Start on port 8080
- Auto-start C++ worker process
- Connect to gRPC hub at 72.60.111.138:50051

### 4. Stop Container
```bash
./docker-stop.sh
```

## Testing Endpoints

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Demo Endpoints
```bash
# Echo test
curl -X POST http://localhost:8080/api/demo/echo \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello from Docker"}'

# Calculate test (Java + C++ processing)
curl -X POST http://localhost:8080/api/demo/calculate \
  -H "Content-Type: application/json" \
  -d '{"value":10}'

# Transform test
curl -X POST http://localhost:8080/api/demo/transform \
  -H "Content-Type: application/json" \
  -d '{"data":"test"}'
```

### C++ Worker Management
```bash
# Get worker status
curl http://localhost:8080/api/cpp-workers/status

# Get worker info
curl http://localhost:8080/api/cpp-workers/info

# Get registered workers
curl http://localhost:8080/api/cpp-workers/workers
```

## Logs

View live logs:
```bash
docker-compose logs -f
```

View Java logs only:
```bash
docker-compose logs -f deepapp-main | grep -v "\[C++ Worker\]"
```

View C++ worker logs only:
```bash
docker-compose logs -f deepapp-main | grep "\[C++ Worker\]"
```

## Configuration

### Environment Variables (docker-compose.yml)
- `JAVA_OPTS`: JVM options (default: `-Xms512m -Xmx2g`)
- `GRPC_SERVER`: gRPC hub address (default: `72.60.111.138:50051`)
- `CPP_CLIENT_ID`: C++ worker client ID (default: `cpp-worker`)
- `SPRING_PROFILES_ACTIVE`: Spring profile (default: `docker`)

### Volumes
- `./logs:/app/logs` - Application logs
- `./src/main/resources/application-docker.yml:/app/config/application-docker.yml:ro` - Docker-specific config

## Development

### Adding New C++ Workers

1. Create worker file: `src/main/cpp/com/deepapp/vn/io/AA/AX/AAAX_XXXX/worker/AAAX_XXXX_W.cpp`
2. Use auto-registration macro:
```cpp
REGISTER_WORKER(deepapp::workers::AAAX_XXXX_Worker, "AAAX_XXXX_W");
```
3. Rebuild Docker image: `./docker-build.sh`

### Adding New Java Modules

1. Create module package: `src/main/java/com/deepapp/vn/io/modules/yourmodule/`
2. Create service and controller classes
3. Inject `CppWorkerClient` for C++ communication
4. Rebuild Docker image: `./docker-build.sh`

## Troubleshooting

### Container won't start
```bash
# Check container logs
docker-compose logs deepapp-main

# Check if port 8080 is available
netstat -tlnp | grep 8080
```

### C++ worker not connecting
```bash
# Check gRPC server accessibility
telnet 72.60.111.138 50051

# Verify worker logs
docker-compose logs deepapp-main | grep "C++ Worker"
```

### Health check failing
```bash
# Check actuator endpoint
docker exec -it deepapp-main curl http://localhost:8080/actuator/health
```

## Clean Up

Remove all containers and images:
```bash
./docker-stop.sh
docker-compose down -v
docker system prune -af
```

## Production Deployment

For production, consider:
1. Use external configuration management
2. Set up log aggregation (ELK, Splunk)
3. Configure resource limits in docker-compose.yml
4. Use Docker secrets for sensitive data
5. Set up monitoring (Prometheus + Grafana)
6. Configure backup for logs volume

## Support

For issues or questions, check:
- Application logs: `./logs/deepapp.log`
- Container logs: `docker-compose logs -f`
- Health endpoint: `http://localhost:8080/actuator/health`
