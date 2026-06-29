#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/opt/memospark}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
ARTIFACT="${ARTIFACT:-}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 127
  fi
}

require_cmd docker
require_cmd curl
require_cmd tar

case "$APP_DIR" in
  "" | "/")
    echo "Refusing to deploy to unsafe APP_DIR: $APP_DIR" >&2
    exit 2
    ;;
esac

mkdir -p "$APP_DIR"

if [ ! -f "$APP_DIR/.env" ]; then
  echo "Missing $APP_DIR/.env. Create it from .env.production.example before deploying." >&2
  exit 2
fi

if [ -z "$ARTIFACT" ]; then
  echo "Missing ARTIFACT path. Upload a deployment tarball before running this script." >&2
  exit 2
fi

if [ ! -f "$ARTIFACT" ]; then
  echo "Missing deployment artifact: $ARTIFACT" >&2
  exit 2
fi

tmp_dir="$(mktemp -d)"
release_dir="$tmp_dir/release"
preserved_env="$tmp_dir/.env"
trap 'rm -rf "$tmp_dir"; rm -f "$ARTIFACT"' EXIT

mkdir -p "$release_dir"
tar -xzf "$ARTIFACT" -C "$release_dir"

if [ ! -f "$release_dir/$COMPOSE_FILE" ]; then
  echo "Deployment artifact does not contain $COMPOSE_FILE." >&2
  exit 2
fi

if [ ! -f "$release_dir/Dockerfile" ]; then
  echo "Deployment artifact does not contain Dockerfile." >&2
  exit 2
fi

cp -p "$APP_DIR/.env" "$preserved_env"
find "$APP_DIR" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
cp -a "$release_dir/." "$APP_DIR/"
cp -p "$preserved_env" "$APP_DIR/.env"

cd "$APP_DIR"

docker compose -f "$COMPOSE_FILE" config >/dev/null
docker compose -f "$COMPOSE_FILE" build app
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans app

# Health check: app port is not mapped to host (nginx is the front door),
# so check inside the container via docker compose exec.
for _ in $(seq 1 40); do
  if docker compose -f "$COMPOSE_FILE" exec -T app \
      curl -fsS http://127.0.0.1:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    docker compose -f "$COMPOSE_FILE" ps app
    exit 0
  fi
  sleep 3
done

docker compose -f "$COMPOSE_FILE" logs --tail=200 app >&2
exit 1
