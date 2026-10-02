#!/bin/bash
# Start a Compose deployment and wait for its services to become ready.
# Usage: start.sh <compose-file> <project-name>

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <compose-file> <project-name>" >&2
  exit 1
fi

COMPOSE=(docker-compose -f "$1" -p "$2")
if ! SERVICES=$("${COMPOSE[@]}" config --services); then
  echo "Deployment failed: could not determine Compose services" >&2
  exit 1
fi

start_stack() {
  if printf '%s\n' "$SERVICES" | grep -qx deployment; then
    # The deployment image populates a shared volume and then exits. Mark it as
    # a successful-completion dependency so Compose --wait accepts exit code 0.
    "${COMPOSE[@]}" -f - up -d --wait --wait-timeout 300 <<'COMPOSE_OVERRIDE'
services:
  manager:
    depends_on:
      deployment:
        condition: service_completed_successfully
COMPOSE_OVERRIDE
  else
    "${COMPOSE[@]}" up -d --wait --wait-timeout 300
  fi
}

echo "Starting the stack and waiting for services to be ready"
if ! start_stack; then
  echo "Deployment failed: services could not start or become ready"

  echo "Deployment container states:"
  "${COMPOSE[@]}" ps -a

  echo "Container health diagnostics:"
  for CONTAINER_ID in $("${COMPOSE[@]}" ps -a -q); do
    docker inspect --format '
{{.Name}}: state={{.State.Status}}, exit={{.State.ExitCode}}
{{if .State.Error}}Error: {{.State.Error}}{{end}}
{{if .State.Health}}Health: {{.State.Health.Status}}
{{if ne .State.Health.Status "healthy"}}Recent health checks:
{{range .State.Health.Log}}  {{.End}} exit={{.ExitCode}}
{{.Output}}
{{end}}{{end}}{{else}}No health check configured
{{end}}' "$CONTAINER_ID"
  done

  exit 1
fi

echo "All deployment services are ready"
