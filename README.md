# Orang Chat 🍊

Orang Chat is a development-stage real-time chat project with a React frontend and a Spring Boot microservices backend.

## Technology stack

### Frontend (`/frontend`)

Versions below are taken from `frontend/package.json`.

- React: `19.2.0` (`react`, `react-dom`)
- React Router: `7.13.1` (`react-router-dom`)
- Material UI: `7.3.9` (`@mui/material`, `@mui/icons-material`)
- Vite: `7.3.1`
- STOMP.js: `7.3.0` (`@stomp/stompjs`)
- SockJS Client: `1.6.1`
- React Hook Form: `7.71.2`

### Backend (`/backend`)

From `backend/pom.xml`:

- Java `21`
- Spring Boot `3.5.11`
- Spring Cloud `2025.0.1`

Backend modules:

- Deployable services (6):
  - `api-gateway`
  - `auth-service`
  - `user-service`
  - `message-service`
  - `chat-service`
  - `notification-service`
- Shared module (1):
  - `shared-library`

## Project structure

```text
.
├── backend/     # Spring Boot multi-module backend
├── frontend/    # React + Vite frontend
├── Run/         # Docker Compose files and environment templates
├── docs/        # Project documentation
├── LICENSE
└── README.md
```

## Setup and running

Runtime setup files are in `Run/`:

- `Run/.env.example`
- `Run/docker-compose-dev.yml`
- `Run/docker-compose-prod.yml`

Example local start:

```bash
cp Run/.env.example Run/.env
docker compose --env-file Run/.env -f Run/docker-compose-dev.yml up --build
```

## Documentation

All links below point to files currently present in this repository:

- [Backend architecture](docs/backend/ARCHITECTURE.md)
- [Backend technology](docs/backend/TECHNOLOGY.md)
- [Backend feature status](docs/backend/feature-status.md)
- [Backend exception handling](docs/backend/exception-handling.md)
- [Frontend architecture](docs/frontend/architecture.md)
- [Frontend technology](docs/frontend/technology.md)
- [Deployment guide](docs/deployment/deployment.md)
- [Security documentation](docs/security_docs/sec.md)
- [Backend README](backend/README.md)
- [Frontend README](frontend/README.md)

## License

This project is licensed under the [MIT License](LICENSE).
