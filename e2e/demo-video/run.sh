#!/usr/bin/env bash
# 시연영상을 만든다: app.jar 를 demo 프로필로 새로 띄우고(샘플 데이터 초기화) → 녹화 → 합성 → 시나리오·대본 문서.
# 장면이 데이터를 바꾸므로(회신 제출, 확정 등) 녹화할 때마다 서버를 새로 띄워야 같은 결과가 나온다.
#
#   e2e/demo-video/run.sh                        # 전체. 무음 out/demo.mp4
#   e2e/demo-video/run.sh --only=suspensions     # 그 장(chapter)만 다시 찍고 전체를 다시 이어 붙인다
#   e2e/demo-video/run.sh --only=flow-branch,chat   # 장면 id 로도 된다
#   e2e/demo-video/run.sh --music=out/bgm.wav    # 배경음 포함(--only 외의 인자는 compose.ts 로 그대로 간다)
#   DEMO_SCENARIO=autotransfer e2e/demo-video/run.sh   # 2차 심사 장별 영상 out/demo-{prep,biz,dev,ops,susp}.mp4
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
root="$(cd "$here/../.." && pwd)"
jar="$root/backend/build/libs/app.jar"
port="${DEMO_PORT:-8080}"
log="${DEMO_LOG:-/tmp/connect-link-demo-video.log}"

record_args=()
compose_args=()
for a in "$@"; do
  case "$a" in
    --only=*) record_args+=("$a") ;;
    *) compose_args+=("$a") ;;
  esac
done

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
# 2차 심사 발표 영상(scenario-autotransfer.md)은 녹화 전에 과제 데이터를 만든다. 1차 영상은 그대로 시드만 쓴다.
if [ "${DEMO_SCENARIO:-}" = "autotransfer" ]; then node "$here/setup-autotransfer.ts"; fi
node "$here/record.ts" "${record_args[@]+"${record_args[@]}"}"
if [ "${DEMO_SCENARIO:-}" = "autotransfer" ]; then
  # 2차 심사는 장표마다 넣을 장별 영상(out/demo-<장>.mp4)만 뽑는다. 길이 맞춤(178초)은 1차 영상 전용이다.
  for chapter in prep biz dev ops susp; do
    node "$here/compose.ts" --chapter="$chapter" "${compose_args[@]+"${compose_args[@]}"}"
  done
else
  node "$here/compose.ts" "${compose_args[@]+"${compose_args[@]}"}"
  node "$here/docs.ts"
fi
