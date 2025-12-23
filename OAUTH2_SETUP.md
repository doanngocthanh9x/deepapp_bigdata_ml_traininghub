# OAuth2 Setup Guide

## 📝 Quick Setup

### 1. Google OAuth2

**Create OAuth App:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create new project or select existing
3. Navigate to **APIs & Services** → **Credentials**
4. Click **Create Credentials** → **OAuth client ID**
5. Application type: **Web application**
6. Add authorized redirect URIs:
   - `http://localhost:8080/login/oauth2/code/google`
   - `http://your-domain:8080/login/oauth2/code/google`
7. Copy **Client ID** and **Client Secret**

**Configure:**
```bash
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"
```

### 2. GitHub OAuth2

**Create OAuth App:**
1. Go to [GitHub Settings](https://github.com/settings/developers)
2. Click **OAuth Apps** → **New OAuth App**
3. Application name: `DeepApp`
4. Homepage URL: `http://localhost:8080`
5. Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
6. Copy **Client ID** and **Client Secret**

**Configure:**
```bash
export GITHUB_CLIENT_ID="your-github-client-id"
export GITHUB_CLIENT_SECRET="your-github-client-secret"
```

## 🚀 Run Application

### Local Development:
```bash
# Set environment variables
export GOOGLE_CLIENT_ID="xxx"
export GOOGLE_CLIENT_SECRET="xxx"
export GITHUB_CLIENT_ID="xxx"
export GITHUB_CLIENT_SECRET="xxx"

# Run Spring Boot
cd /root/deepapp/deepapp_main
mvn spring-boot:run
```

### Docker:
```bash
# Update docker-compose.yml with OAuth credentials
docker-compose up -d
```

## 🧪 Test OAuth2

1. **Login Page:** http://localhost:8080/login
2. **Click "Continue with Google"** or **"Continue with GitHub"**
3. **After login, access:** http://localhost:8080/AA/A0/AAA0_0100

## 📡 API Endpoints

### Public (No Auth):
- `GET /actuator/health` - Health check
- `POST /api/demo/echo` - Demo echo
- `GET /login` - Login page

### Protected (OAuth2 Required):
- `GET /AA/A0/AAA0_0100` - Get user profile
- `POST /AA/A0/AAA0_0100` - Process data with C++ worker
  ```json
  {
    "eventType": "process",
    "data": "your data here"
  }
  ```

## 🔧 Configuration Files

**application.yml:**
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
```

**docker-compose.yml:**
```yaml
environment:
  - GOOGLE_CLIENT_ID=your-id
  - GOOGLE_CLIENT_SECRET=your-secret
  - GITHUB_CLIENT_ID=your-id
  - GITHUB_CLIENT_SECRET=your-secret
```

## 🌐 Production Setup

1. **Update redirect URIs** in Google/GitHub with production domain
2. **Enable HTTPS** for OAuth2 callback
3. **Store secrets securely** (AWS Secrets Manager, Azure Key Vault, etc.)
4. **Enable CSRF protection** in SecurityConfig.java

## 🔐 Security Features

- ✅ OAuth2 authentication (Google, GitHub)
- ✅ JWT token handling  
- ✅ Session management
- ✅ CSRF protection (can enable)
- ✅ Secure cookie handling
- ✅ Role-based access control (extensible)

## 📚 Architecture

```
User → Login Page → OAuth Provider → Callback → Spring Security → AAA0_0100_trx → C++ Worker → Response
```

**Flow:**
1. User clicks "Login with Google/GitHub"
2. Redirected to OAuth provider
3. User authorizes application
4. OAuth provider redirects back with code
5. Spring Security exchanges code for token
6. User info stored in session
7. Access protected endpoints with user context
8. AAA0_0100_trx sends user info to C++ worker

## 🛠️ Troubleshooting

**"Invalid redirect_uri":**
- Check redirect URI in OAuth app settings
- Must exactly match configured callback URL

**"Unauthorized":**
- Verify client ID and secret are correct
- Check environment variables are loaded

**"Connection refused":**
- Ensure application is running on port 8080
- Check firewall settings
