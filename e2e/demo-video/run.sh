#!/usr/bin/env bash
# 시연영상을 처음부터 끝까지 만든다: app.jar 를 demo 프로필로 새로 띄우고(샘플 데이터 초기화) → 녹화 → 합성.
# 장면이 데이터를 바꾸므로(회신 제출, 확정 등) 녹화할 때마다 서버를 새로 띄워야 같은 결과가 나온다.
#
#   e2e/demo-video/run.sh                 # 무음 demo.mp4
#   e2e/demo-video/run.sh --music=out/bgm.wav   # 배경음 포함(인자는 compose.ts 로 그대로 간다)
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
root="$(cd "$here/../.." && pwd)"
jar="$root/backend/build/libs/app.jar"
port="${DEMO_PORT:-8080}"
log="${DEMO_LOG:-/tmp/connect-link-demo-video.log}"

[ -f "$jar" ] || { echo "app.jar 가 없습니다. ./gradlew :backend:bootJar -x test 로 먼저 만드세요."; exit 1; }

# 이전 app.jar 서버를 내린다. 셸(명령줄에 같은 글자가 있을 수 있다)을 잘못 죽이지 않도록 java 프로세스만 고른다.
for pid in $(pgrep -x java || true); do
  if tr '\0' ' ' < "/proc/$pid/cmdline" 2>/dev/null | grep -q 'app\.jar'; then kill "$pid" || true; fi
done
for _ in $(seq 1 20); do
  curl -fs "http://localhost:$port/api/v1/health" > /dev/null 2>&1 || break
  sleep 1
done
if curl -fs "http://localhost:$port/api/v1/health" > /dev/null 2>&1; then
  echo "포트 $port 를 다른 프로세스가 쓰고 있습니다. 내리고 다시 실행하세요."; exit 1
fi

nohup java -jar "$jar" --server.port="$port" > "$log" 2>&1 &
server=$!
trap 'kill $server 2>/dev/null || true' EXIT

for _ in $(seq 1 90); do
  if curl -fs "http://localhost:$port/api/v1/health" > /dev/null 2>&1; then break; fi
  sleep 1
done
curl -fs "http://localhost:$port/api/v1/health" > /dev/null || { echo "서버가 뜨지 않았습니다: $log"; exit 1; }

export DEMO_BASE_URL="http://localhost:$port"
node "$here/record.ts"
node "$here/compose.ts" "$@"
