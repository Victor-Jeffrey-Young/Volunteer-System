#!/usr/bin/env bash
# ============================================================
# 一键把数据库重置为「精编演示数据」
#
#   ./scripts/reset-demo-data.sh           交互确认后执行
#   ./scripts/reset-demo-data.sh --yes     跳过确认（演示前定时重置用）
#   ./scripts/reset-demo-data.sh --no-backup
#
# 做三件事：
#   1. 把当前库 mysqldump 备份到 ./backups/（默认做，可关）
#   2. 执行 sql/demo-data.sql：清空业务表 + 写入一套自洽的展示数据
#   3. 打印演示账号与恢复方法
#
# 数据库入口自动探测：容器 volunteer-mysql 在跑就走 docker exec，
# 否则回落到本机 mysql 客户端（连 127.0.0.1:$DB_PORT）。
# 凭据从 .env 读取，读不到就用开发默认值。
# ============================================================
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="$ROOT_DIR/backups"
CONTAINER="volunteer-mysql"
ASSUME_YES=0
DO_BACKUP=1

for arg in "$@"; do
    case "$arg" in
        -y|--yes)      ASSUME_YES=1 ;;
        --no-backup)   DO_BACKUP=0 ;;
        -h|--help)     sed -n '2,14p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *)             echo "未知参数：${arg}（用 --help 看用法）"; exit 1 ;;
    esac
done

C_RESET='\033[0m'; C_GREEN='\033[32m'; C_YELLOW='\033[33m'; C_RED='\033[31m'; C_CYAN='\033[36m'
info() { printf "${C_CYAN}[重置]${C_RESET} %s\n" "$*"; }
ok()   { printf "${C_GREEN}[重置]${C_RESET} %s\n" "$*"; }
warn() { printf "${C_YELLOW}[重置]${C_RESET} %s\n" "$*"; }
die()  { printf "${C_RED}[重置] %s${C_RESET}\n" "$*"; exit 1; }

# ---------- 读取 .env（只取需要的键，不 source 整个文件）----------
read_env() {
    local key="$1" default="$2" line
    line="$(grep -E "^${key}=" "$ROOT_DIR/.env" 2>/dev/null | tail -1 || true)"
    if [ -n "$line" ]; then printf '%s' "${line#*=}" | tr -d '\r'; else printf '%s' "$default"; fi
}

DB_NAME="$(read_env DB_NAME volunteer_db)"
DB_USER="$(read_env DB_USERNAME root)"
DB_PASS="$(read_env DB_PASSWORD 123456)"
DB_PORT="$(read_env DB_PORT 3306)"

[ -f "$ROOT_DIR/sql/demo-data.sql" ] || die "找不到 sql/demo-data.sql"

# ---------- 探测数据库入口 ----------
if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$CONTAINER"; then
    MODE="docker($CONTAINER)"
    run_sql() { docker exec -i -e MYSQL_PWD="$DB_PASS" "$CONTAINER" \
        mysql --default-character-set=utf8mb4 -u"$DB_USER" "$DB_NAME"; }
    dump_db() { docker exec -e MYSQL_PWD="$DB_PASS" "$CONTAINER" \
        mysqldump --single-transaction --default-character-set=utf8mb4 -u"$DB_USER" "$DB_NAME"; }
else
    command -v mysql >/dev/null 2>&1 || die "容器 $CONTAINER 没在运行，本机也没装 mysql 客户端"
    MODE="local(127.0.0.1:$DB_PORT)"
    run_sql() { MYSQL_PWD="$DB_PASS" mysql --default-character-set=utf8mb4 \
        -h127.0.0.1 -P"$DB_PORT" -u"$DB_USER" "$DB_NAME"; }
    dump_db() { MYSQL_PWD="$DB_PASS" mysqldump --single-transaction --default-character-set=utf8mb4 \
        -h127.0.0.1 -P"$DB_PORT" -u"$DB_USER" "$DB_NAME"; }
fi
info "数据库入口：${MODE}，库名：${DB_NAME}"

# ---------- 连通性检查 ----------
if ! run_sql <<< "SELECT 1;" >/dev/null 2>&1; then
    die "连不上数据库，请确认容器已启动、.env 里的口令正确"
fi

# ---------- 备份 ----------
if [ "$DO_BACKUP" = "1" ]; then
    mkdir -p "$BACKUP_DIR"
    BACKUP_FILE="$BACKUP_DIR/volunteer_$(date +%Y%m%d-%H%M%S).sql.gz"
    info "备份当前数据 -> $BACKUP_FILE"
    if dump_db 2>/dev/null | gzip > "$BACKUP_FILE"; then
        [ -s "$BACKUP_FILE" ] || warn "备份文件是空的（当前库可能本来就没有数据）"
        ok "备份完成：$(du -h "$BACKUP_FILE" | cut -f1)"
    else
        warn "备份失败（继续执行前请确认你真的不需要当前数据）"
        [ "$ASSUME_YES" = "1" ] || die "备份失败，已中止。确实要继续可加 --no-backup"
    fi
fi

# ---------- 确认 ----------
if [ "$ASSUME_YES" != "1" ]; then
    printf "${C_YELLOW}⚠️  将清空全部业务表（用户/活动/报名/商品/兑换/心愿/公告）并写入演示数据。继续？[y/N] ${C_RESET}"
    read -r ans
    case "$ans" in
        y|Y|yes|YES) ;;
        *) info "已取消，未做任何修改"; exit 0 ;;
    esac
fi

# ---------- 导入 ----------
info "导入 sql/demo-data.sql ..."
if run_sql < "$ROOT_DIR/sql/demo-data.sql"; then
    ok "演示数据导入完成"
else
    die "导入失败：请检查上面的 SQL 报错（常见原因是库结构与 sql/ 迁移脚本不一致）"
fi

cat <<'EOF'

演示账号（口令统一：Demo@123456）
  admin       管理员 —— 发布活动、审核报名与心愿、一键发工时、核销兑换码
  volunteer1  志愿者 —— 32h 老手：有已签到可签退的进行中活动、一条待发工时的记录
  volunteer2  志愿者 —— 新手：有可现场签到的活动、一条待核销的兑换码
  resident1   居民   —— 发布了覆盖 7 种状态的心愿，可现场走完整条状态机

建议演示动线：居民发心愿 → 管理员审核心愿 → 志愿者认领/完成 → 居民确认点赞 →
管理员结算发奖 → 志愿者兑换商品 → 管理员扫码核销 → 志愿者签到签退 → 管理员发工时。

EOF
