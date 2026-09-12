#!/usr/bin/env bash
# ============================================================
# Volunteer-System 本地开发一键启动脚本（VSCode / 终端通用）
#
#   ./scripts/dev.sh            启动后端(8081) + 前端(5173)
#   ./scripts/dev.sh backend    只启动后端
#   ./scripts/dev.sh frontend   只启动前端
#   ./scripts/dev.sh --help     查看帮助
#
# 特性：
#   * 自动检查 MySQL 容器、JDK、Node、端口占用
#   * 输出带 [后端]/[前端] 前缀，日志同时落盘，方便排查
#   * Ctrl+C 一次停掉全部子进程（含 Maven 派生的 java 进程）
#
# 兼容 macOS 自带的 bash 3.2，不依赖 bash 4+ 语法。
# ============================================================
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WEB_DIR="$ROOT_DIR/volunteer-web"
LOG_DIR="${TMPDIR:-/tmp}/volunteer-system-dev"
BACKEND_PORT=8081
FRONTEND_PORT=5173
MYSQL_CONTAINER="volunteer-mysql"
JAVA_VERSION="17"

# ---------- 输出着色 ----------
C_RESET='\033[0m'; C_RED='\033[31m'; C_GREEN='\033[32m'
C_YELLOW='\033[33m'; C_BLUE='\033[34m'; C_CYAN='\033[36m'

info()  { printf "${C_CYAN}[dev]${C_RESET} %s\n" "$*"; }
ok()    { printf "${C_GREEN}[dev]${C_RESET} %s\n" "$*"; }
warn()  { printf "${C_YELLOW}[dev]${C_RESET} %s\n" "$*"; }
fail()  { printf "${C_RED}[dev] %s${C_RESET}\n" "$*"; }

usage() {
  sed -n '3,12p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
  exit 0
}

# ---------- 参数解析 ----------
TARGET="all"
case "${1:-}" in
  ""|all)      TARGET="all" ;;
  backend|-b)  TARGET="backend" ;;
  frontend|-f) TARGET="frontend" ;;
  -h|--help)   usage ;;
  *)           fail "未知参数：$1"; usage ;;
esac

# ---------- 进程管理 ----------
PIDS=""
READER_PIDS=""
LAST_PID=""

kill_tree() {
  local pid="$1" child
  for child in $(pgrep -P "$pid" 2>/dev/null); do
    kill_tree "$child"
  done
  kill "$pid" 2>/dev/null
}

cleanup() {
  trap - INT TERM EXIT
  echo ""
  info "正在停止服务 ..."
  local pid
  for pid in $PIDS; do kill_tree "$pid"; done
  for pid in $READER_PIDS; do kill_tree "$pid"; done
  pkill -P $$ 2>/dev/null   # 兜底：清掉残留的 tail/sed 读取进程
  wait 2>/dev/null
  ok "已全部停止。日志保留在 $LOG_DIR"
  exit 0
}

# start_service <前缀> <颜色> <日志文件> <工作目录> <命令...>
start_service() {
  local name="$1" color="$2" logfile="$3" workdir="$4"; shift 4
  : > "$logfile"
  # 服务输出写入日志文件，再由 tail 读出加前缀打印到终端
  # （刻意不用进程替换 >(...)，兼容性更好，也让日志可回看）
  ( cd "$workdir" && exec "$@" ) > "$logfile" 2>&1 &
  LAST_PID=$!
  PIDS="$PIDS $LAST_PID"
  tail -n +1 -f "$logfile" 2>/dev/null | sed -u -e "s/^/${color}[${name}]${C_RESET} /" &
  READER_PIDS="$READER_PIDS $!"
}

port_in_use() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

# wait_http <url> <服务名> <超时秒> <pid> <日志文件>
wait_http() {
  local url="$1" name="$2" timeout="$3" pid="$4" logfile="$5"
  local waited=0 code
  while [ "$waited" -lt "$timeout" ]; do
    if ! kill -0 "$pid" 2>/dev/null; then
      fail "$name 进程提前退出，最后 30 行日志："
      tail -n 30 "$logfile"
      return 1
    fi
    if grep -q -e "APPLICATION FAILED TO START" -e "BUILD FAILURE" -e "Error starting ApplicationContext" "$logfile" 2>/dev/null; then
      fail "$name 启动失败，最后 30 行日志："
      tail -n 30 "$logfile"
      return 1
    fi
    code="$(curl -sk -o /dev/null -m 2 -w '%{http_code}' "$url" 2>/dev/null)"
    [ -n "$code" ] && [ "$code" != "000" ] && return 0
    sleep 2
    waited=$((waited + 2))
  done
  fail "$name 在 ${timeout}s 内未就绪，请查看日志：$logfile"
  return 1
}

trap cleanup INT TERM

# ---------- 环境检查 ----------
printf "\n${C_BLUE}=== 启动 Volunteer-System 开发环境（%s）===${C_RESET}\n\n" "$TARGET"
mkdir -p "$LOG_DIR"

# JAVA_HOME 优先用环境变量；没设置时用系统自带的 JDK 探测器（macOS 的 java_home、
# Linux 的常见安装位置），避免把某台机器的绝对路径写死进仓库。
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
  JAVA_HOME="$(/usr/libexec/java_home -v "$JAVA_VERSION" 2>/dev/null || true)"
  [ -n "$JAVA_HOME" ] && export JAVA_HOME
fi
if [ -z "${JAVA_HOME:-}" ] && [ -d "/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64" ]; then
  export JAVA_HOME="/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64"
fi

if [ "$TARGET" = "all" ] || [ "$TARGET" = "backend" ]; then
  command -v java >/dev/null 2>&1 || { fail "未找到 java，请先安装 JDK $JAVA_VERSION（macOS: brew install openjdk@$JAVA_VERSION）"; exit 1; }
  java_major="$(java -version 2>&1 | head -1 | sed -n 's/.*version "\([0-9]*\).*/\1/p')"
  [ "$java_major" = "17" ] || warn "当前 JDK 主版本为 ${java_major:-未知}，项目要求 17"
  info "JDK：$JAVA_HOME"

  if port_in_use "$BACKEND_PORT"; then
    fail "端口 $BACKEND_PORT 已被占用（后端可能已在运行）："
    lsof -nP -iTCP:"$BACKEND_PORT" -sTCP:LISTEN
    exit 1
  fi

  if command -v docker >/dev/null 2>&1; then
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$MYSQL_CONTAINER"; then
      ok "MySQL 容器 $MYSQL_CONTAINER 已在运行（3306）"
    elif docker ps -a --format '{{.Names}}' 2>/dev/null | grep -qx "$MYSQL_CONTAINER"; then
      info "MySQL 容器未运行，正在启动 ..."
      docker start "$MYSQL_CONTAINER" >/dev/null && ok "MySQL 容器已启动" || warn "MySQL 容器启动失败，请手动检查"
    else
      warn "未找到容器 $MYSQL_CONTAINER，后端连接数据库可能失败"
    fi
  else
    warn "未检测到 docker，跳过 MySQL 容器检查"
  fi
fi

if [ "$TARGET" = "all" ] || [ "$TARGET" = "frontend" ]; then
  command -v npm >/dev/null 2>&1 || { fail "未找到 npm，请先安装 Node.js 18+"; exit 1; }
  info "Node：$(node -v) / npm：$(npm -v)"

  if port_in_use "$FRONTEND_PORT"; then
    fail "端口 $FRONTEND_PORT 已被占用（前端可能已在运行）"
    exit 1
  fi

  if [ ! -d "$WEB_DIR/node_modules" ]; then
    info "未检测到 node_modules，正在执行 npm install（首次较慢）..."
    ( cd "$WEB_DIR" && npm install ) || { fail "npm install 失败"; exit 1; }
  fi
fi

# ---------- 启动 ----------
BACKEND_PID=""
FRONTEND_PID=""

if [ "$TARGET" = "all" ] || [ "$TARGET" = "backend" ]; then
  BACKEND_LOG="$LOG_DIR/backend.log"
  info "启动后端：./mvnw spring-boot:run  ->  $BACKEND_LOG"
  start_service "后端" "$C_GREEN" "$BACKEND_LOG" "$ROOT_DIR" ./mvnw -B spring-boot:run
  BACKEND_PID="$LAST_PID"
fi

if [ "$TARGET" = "all" ] || [ "$TARGET" = "frontend" ]; then
  FRONTEND_LOG="$LOG_DIR/frontend.log"
  info "启动前端：npm run dev  ->  $FRONTEND_LOG"
  start_service "前端" "$C_CYAN" "$FRONTEND_LOG" "$WEB_DIR" npm run dev
  FRONTEND_PID="$LAST_PID"
fi

echo ""
RC=0
if [ -n "$BACKEND_PID" ]; then
  info "等待后端就绪（首次编译可能需要 1-2 分钟）..."
  wait_http "http://127.0.0.1:$BACKEND_PORT/doc.html" "后端" 240 "$BACKEND_PID" "$LOG_DIR/backend.log" || RC=1
fi
if [ -n "$FRONTEND_PID" ] && [ "$RC" = "0" ]; then
  info "等待前端就绪 ..."
  wait_http "https://127.0.0.1:$FRONTEND_PORT/" "前端" 90 "$FRONTEND_PID" "$LOG_DIR/frontend.log" || RC=1
fi

if [ "$RC" != "0" ]; then
  fail "启动失败，正在清理已启动的进程 ..."
  cleanup
fi

echo ""
ok "服务已就绪 ✔"
[ -n "$BACKEND_PID" ]  && printf "    后端接口文档   http://localhost:%s/doc.html\n" "$BACKEND_PORT"
[ -n "$FRONTEND_PID" ] && printf "    前端页面       https://localhost:%s  ${C_YELLOW}(自签名证书，浏览器点“继续访问”)${C_RESET}\n" "$FRONTEND_PORT"
printf "    实时日志       tail -f %s/*.log\n" "$LOG_DIR"
printf "    停止服务       ${C_YELLOW}Ctrl+C${C_RESET}\n\n"

wait
