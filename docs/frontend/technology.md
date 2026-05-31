# Frontend Technology Stack

## Core Technologies

### Framework & Build
- **React**: UI library for building user interfaces
- **Vite**: Next-generation frontend build tool and dev server
- **Node.js**: JavaScript runtime environment

### Package Manager
- **npm** or **yarn**: Dependency management

## Dependencies
See [package.json](../package.json) for complete list of dependencies and versions.

## Development Tools

### Linting & Code Quality
- **ESLint**: JavaScript linting (configured in [eslint.config.js](../eslint.config.js))

### Version Control
- **Git**: Source code management

## Deployment

### Containerization
- **Docker**: Application containerization
- **Nginx**: Web server for production deployment (config: [nginx.conf](../nginx.conf))

## API Integration
- Communication with backend services through REST APIs
- Service layer abstracts API calls (see [services/](../src/services/))

## Browser Support
Targets modern browsers with ES6+ support.

## Performance Features
- Code splitting via Vite
- Lazy loading of components and routes
- Service Worker support for PWA capabilities (see [sw.js](../public/sw.js))
