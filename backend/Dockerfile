{
  "name": "Clinica Citas Dev",
  "image": "mcr.microsoft.com/devcontainers/java:17",
  "features": {
    "ghcr.io/devcontainers/features/docker-in-docker:2": {}
  },
  "customizations": {
    "vscode": {
      "extensions": [
        "vscjava.vscode-java-pack",
        "vscjava.vscode-spring-boot-dashboard"
      ]
    }
  },
  "forwardPorts": [8080, 5432, 15672],
  "postCreateCommand": "docker-compose up -d postgres rabbitmq"
}