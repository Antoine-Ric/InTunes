#!/usr/bin/env bash
set -euo pipefail
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$repo_root"
if [[ ! -f backend/target/si-tunes-api-0.1.0-SNAPSHOT.jar || ! -d frontend/node_modules ]]; then
  echo 'Install first: backend/./mvnw verify and frontend/npm ci (see README).' >&2
  exit 1
fi
children=()
cleanup() {
  for child in "${children[@]}"; do kill "$child" 2>/dev/null || true; done
  for child in "${children[@]}"; do wait "$child" 2>/dev/null || true; done
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
if [[ -z "${LASTFM_API_KEY:-}" ]]; then
  echo 'LASTFM_API_KEY is missing. The UI and API will run, but real suggestions return a configuration error.' >&2
fi
java -jar backend/target/si-tunes-api-0.1.0-SNAPSHOT.jar &
children+=("$!")
python3 scripts/wait-for-http.py "http://127.0.0.1:${PORT:-8080}/actuator/health"
cd frontend
API_PROXY_TARGET="http://127.0.0.1:${PORT:-8080}" ./node_modules/.bin/vite --host 127.0.0.1 --port 5173 &
children+=("$!")
wait -n "${children[@]}"
