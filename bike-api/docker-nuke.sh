#!/usr/bin/env bash
set -euo pipefail

echo "⚠️  This will DELETE: all containers, images, volumes, networks, and build cache."
read -r -p "Type YES to continue: " confirm
if [[ "${confirm}" != "YES" ]]; then
  echo "Aborted."
  exit 1
fi

echo "🧹 Stopping all running containers..."
docker ps -q | xargs -r docker stop

echo "🧨 Removing all containers..."
docker ps -aq | xargs -r docker rm -f

echo "🗑️  Removing all images..."
docker images -aq | xargs -r docker rmi -f

echo "🧼 Removing all volumes..."
docker volume ls -q | xargs -r docker volume rm -f

echo "🧯 Removing all non-default networks..."
docker network ls --format '{{.Name}}' \
  | grep -vE '^(bridge|host|none)$' \
  | xargs -r docker network rm

echo "🧽 Pruning everything else (including build cache)..."
docker system prune -a --volumes -f

echo "✅ Docker cleanup complete."
