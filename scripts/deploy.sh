#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/opt/memospark}"
REPO_URL="${REPO_URL:-https://github.com/hpliStartAgain/memospark.git}"
BRANCH="${BRANCH:-main}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 127
  fi
}

require_cmd git
require_cmd docker
require_cmd curl

mkdir -p "$APP_DIR"

if [ ! -d "$APP_DIR/.git" ]; then
  tmp_dir="$(mktemp -d)"
  trap 'rm -rf "$tmp_dir"' EXIT
  git clone --depth 1 --branch "$BRANCH" "$REPO_URL" "$tmp_dir/repo"
  cp -a "$tmp_dir/repo/." "$APP_DIR/"
else
  cd "$APP_DIR"
  git remote set-url origin "$REPO_URL"
  git fetch --prune origin "$BRANCH"
  git checkout -B "$BRANCH" "origin/$BRANCH"
  git reset --hard "origin/$BRANCH"
fi

cd "$APP_DIR"

if [ ! -f .env ]; then
  echo "Missing $APP_DIR/.env. Create it from .env.production.example before deploying." >&2
  exit 2
fi

docker compose -f "$COMPOSE_FILE" config >/dev/null
docker compose -f "$COMPOSE_FILE" build app
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans app

for _ in $(seq 1 40); do
  if curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'; then
    docker compose -f "$COMPOSE_FILE" ps app
    exit 0
  fi
  sleep 3
done

docker compose -f "$COMPOSE_FILE" logs --tail=200 app >&2
exit 1
