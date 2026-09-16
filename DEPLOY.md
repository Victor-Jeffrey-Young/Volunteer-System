# 部署手册：把项目放到公网做演示

> 目标：在一台干净的 VPS 上，用三个容器（MySQL + 后端 + 前端 nginx）跑起来，
> 并且满足三件事：**能打开页面**、**扫码签到能用**、**不会被路人拿走管理员权限**。
>
> 相关文件：`docker-compose.yml`、`.env.example`、`deploy/Dockerfile.backend`、
> `deploy/Dockerfile.frontend`、`deploy/nginx.conf`、`sql/`。

---

## 0. 为什么这份手册里有几个"必须"

| 必须做的事 | 不做会怎样 |
|---|---|
| 设置 `JWT_SECRET` | 源码里的默认密钥随公开仓库一起公开了，任何人可签一个 `role=ADMIN` 的 token 直接调管理接口 |
| 设置 `DB_PASSWORD` 且 3306 只绑本机 | `root/123456` + 公网 3306 = 数据库几分钟内被接管 |
| 上 HTTPS | 扫码签到依赖浏览器 `getUserMedia`，它只在安全上下文（HTTPS/localhost）可用，http 下摄像头调不起来 |
| 用 nginx 托管前端并反代 `/api` | 后端不托管前端产物，前端用的是 `createWebHistory`：直接 `java -jar` 会「没有页面」，刷新子路由还会 404 |

正因为前两条太容易被忽略，`application-prod.yml` 里这两个变量**故意没有默认值**：
没配就启动失败（`Could not resolve placeholder 'JWT_SECRET'`），而不是悄悄用一个公开的弱默认值跑起来。

---

## 1. 选路线 + 前置准备

### 1.1 三条路线（按「省心程度」排序）

| 路线 | 成本 | 适合 | 代价 / 注意 |
|---|---|---|---|
| **A. 学生额度 VPS** —— DigitalOcean $200 信用（有效期 1 年）、Azure for Students $100、Oracle 永久免费 | **0 元** | 长期挂一个能写进简历的链接 | 要绑卡验证；**选 2GB 内存**机型（1GB 按 2.1 节做适配）。Oracle 的免费 ARM 已被腰斩到 2 OCPU / 12GB，且经常抢不到容量，别当唯一方案 |
| **B. Cloudflare Tunnel + 一台常开的机器** | **0 元** | 不想绑卡、家里/宿舍有常开的机器 | 机器关机就没了；不用公网 IP、不用开端口，自动给 HTTPS |
| **C. GitHub Codespaces**（见第 9 节） | **0 元**（学生额度） | 面试现场临时演示，自带 HTTPS，摄像头可用 | 临时性：闲置会停；数据随环境重建 |

> **域名**：Namecheap 学生包送 `.me` 一年（GitHub Student Pack 里领）；HTTPS 用 Let's Encrypt（第 4 节）。
> **CI/CD 不花钱**：公开仓库的 GitHub Actions 分钟数免费，镜像可以推到 GHCR（见第 10 节）。

### 1.2 动手前的准备清单

- [ ] **代码已推到 GitHub** —— 服务器 `git clone` 到的是远端版本，本地没推的提交不会过去
- [ ] 目标机器：内存 ≥2GB（1GB 严格照 2.1 节）、放行 **80/443**、**绝不放行 3306**
- [ ] 生成两个密钥：`openssl rand -base64 48`（`JWT_SECRET`）、`openssl rand -base64 18`（`DB_PASSWORD`）
- [ ] 域名与 A 记录（只做内网验证可跳过，先用自签证书）
- [ ] 记住迁移脚本必须执行：`sql/` 下三个脚本 —— 漏掉就没有唯一索引与 CHECK 兜底
- [ ] 演示数据：`bash scripts/reset-demo-data.sh --yes`（执行前会自动备份现有数据）

### 1.3 服务器准备

- 一台能装 Docker 的 Linux 服务器（1 核 2G 起，演示够用）
- 一个域名，A 记录指向服务器 IP（要 HTTPS 就得有域名；只做内网验证可以跳过）
- 服务器放行 **80 / 443**（SSH 端口按需）；**不要放行 3306**

```bash
# 服务器上装 Docker（Debian/Ubuntu）
curl -fsSL https://get.docker.com | sh
```

### 1.4 上线后必须复核的两件事

1. **`JWT_SECRET` 必须是新生成的**：源码里那个默认密钥随公开仓库一起公开了，沿用它等于把管理员
   权限送给任何看过仓库的人。自检：用默认值伪造一个 `role=ADMIN` 的 token 调 `/api/user/page`，
   应当返回 401/403（第 3 节第 4 条给了命令）。
2. **迁移脚本真的执行了**：`SHOW CREATE TABLE sys_registration;` 应看到唯一索引
   `uk_user_activity_active`；`SHOW CREATE TABLE sys_goods;` 应看到
   `ck_goods_stock_non_negative`。看不到就说明少跑了一次脚本，并发兜底少两层。

---

## 2. 五分钟部署

```bash
git clone https://github.com/Victor-Jeffrey-Young/Volunteer-System.git
cd Volunteer-System

# 1) 环境变量：JWT_SECRET 用随机值，DB_PASSWORD 换成强口令
cp .env.example .env
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')|" .env
sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=$(openssl rand -base64 18 | tr -d '\n')|" .env

# 2) 证书（只想先跑起来看：用自签证书，浏览器会警告，但 HTTPS 链路成立）
mkdir -p deploy/certs
openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
  -keyout deploy/certs/privkey.pem -out deploy/certs/fullchain.pem \
  -subj "/CN=$(grep -oP '(?<=^DOMAIN=).*' .env || echo localhost)"   # 正式证书见第 4 步

# 3) 起容器（首次会构建镜像，约 3–8 分钟）
docker compose up -d --build

# 4) 执行数据库迁移（只做一次；漏做会少掉唯一索引与 CHECK 兜底）
for f in sql/*.sql; do
  echo "▶ $f"
  docker exec -i volunteer-mysql mysql -uroot -p"$(grep -oP '(?<=^DB_PASSWORD=).*' .env)" volunteer_db < "$f"
done

# 5) 导入精编演示数据（可选，会把库清成一套干净的展示数据，执行前会自动备份）
bash scripts/reset-demo-data.sh --yes
```

访问 `https://<你的域名>/` 即可。接口文档在 `/doc.html`。

### 2.1 如果是最便宜的 1GB 机型（先看这段）

**默认配置已经按 1GB 适配**，不用改任何东西：JVM 堆上限 50% + SerialGC + 线程栈 512k、
MySQL buffer pool 128M、最大连接 50、关闭 `performance_schema`（能省 100~200MB）。
机器是 2GB / 4GB 想调回去，改 `.env` 里的 `JAVA_TOOL_OPTIONS` 与 `MYSQL_*`
即可 —— `.env.example` 里按机型给了推荐值。

**但不要在 1GB 机器上构建镜像**：Maven 编译和 Vite 打包各自都要 1GB 上下内存，
构建中途会被 OOM Killer 干掉。两种做法：

**做法一（推荐）：本地构建，传上去 load**

```bash
# 本机（已经构建过就跳过第一条）
docker compose build
docker save volunteer-system-backend:latest volunteer-system-frontend:latest | gzip > images.tgz
scp images.tgz root@<服务器IP>:/root/

# 服务器：只需要仓库里的 compose / nginx 配置 / sql，不需要在本地编译
gunzip -c images.tgz | docker load
docker compose up -d          # 不加 --build：两个服务写了 pull_policy: never，直接用 load 进来的镜像
```

**做法二：给服务器加 2GB swap 再构建**

```bash
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

**部署后瞄一眼内存占用：**

```bash
docker stats --no-stream     # backend / mysql 的 MEM USAGE
free -h                      # 还剩多少可用
```

> 想再省一点：把 `DB_POOL_SIZE` 从 10 调到 5（`.env` 里加一行），
> 演示场景并发很低，5 个连接足够。

---

## 3. 上线验收清单（四条，缺一不可）

```bash
# ① 页面能打开（返回前端 HTML）
curl -kI https://<域名>/ | head -1

# ② 接口通了：故意用错密码登录，应该返回业务错误而不是 502/504
curl -k -X POST https://<域名>/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"wrong"}'

# ③ SPA 深链不白屏（返回的还是前端 HTML，而不是 nginx 404）
curl -k https://<域名>/admin/dashboard | head -3

# ④ 数据库没有暴露到公网（本机执行，应连接失败/超时）
nc -vz <服务器IP> 3306
```

再手工确认两件：**手机 4G 打开页面正常**、**扫码签到能调起摄像头**（这条同时验证了 HTTPS 生效）。

还有一个安全自检：用源码里那个公开的默认密钥伪造 token 去调管理接口，应当被拒。

```bash
# 在任意机器上，用公开默认密钥签一个 ADMIN token（jjwt 或在线工具均可），然后：
curl -k -H "Authorization: Bearer <伪造的 token>" https://<域名>/api/user/page
# 期望：401/403；若返回了用户列表，说明 JWT_SECRET 没生效，立刻回第 2 步
```

---

## 4. 正式证书（Let's Encrypt）

```bash
# 宿主机安装 certbot 并申请（先停掉 nginx 容器的 80 占用）
docker compose stop frontend
certbot certonly --standalone -d <域名> --agree-tos -m <邮箱>

# 把证书拷进 compose 挂载的目录（nginx 容器只挂 deploy/certs）
cp /etc/letsencrypt/live/<域名>/fullchain.pem deploy/certs/
cp /etc/letsencrypt/live/<域名>/privkey.pem  deploy/certs/
docker compose start frontend

# 续期自动化：certbot 到期前会自动续，续完把新证书拷进去并让 nginx 重载
cat >/etc/letsencrypt/renewal-hooks/deploy/volunteer.sh <<'HOOK'
#!/bin/sh
cd /opt/Volunteer-System          # 改成你的部署目录
cp /etc/letsencrypt/live/<域名>/fullchain.pem deploy/certs/
cp /etc/letsencrypt/live/<域名>/privkey.pem  deploy/certs/
docker compose exec -T frontend nginx -s reload
HOOK
chmod +x /etc/letsencrypt/renewal-hooks/deploy/volunteer.sh
```

> 想更省事也可以用 Caddy 直接把 443 接管（自动申请与续期），把 `deploy/nginx.conf` 换掉即可，
> 其余（SPA 兜底、反代规则）思路一致。

---

## 5. 日常运维

```bash
# 日志（后端：业务日志；前端 nginx：访问日志与反代错误）
docker compose logs -f backend
docker compose logs -f frontend

# 备份数据库（建议加进 crontab，每天一次，保留 7 天）
docker exec volunteer-mysql mysqldump -uroot -p"$DB_PASSWORD" volunteer_db \
  | gzip > ~/backup/volunteer_$(date +%F).sql.gz

# 演示被刷坏了：一键重置成干净数据（脚本会先自动备份）
bash scripts/reset-demo-data.sh

# 发版更新
git pull && docker compose up -d --build

# 回滚到上一个提交
git log --oneline -5 && git checkout <上一个提交> && docker compose up -d --build
```

**上传的文件**放在宿主机的 `./files`（compose 里挂到容器的 `/data/files`），
重建容器不会丢；备份时记得连它一起打包。

---

## 6. 常见问题

| 现象 | 原因与处理 |
|---|---|
| 启动就报 `Could not resolve placeholder 'JWT_SECRET'` | 这就是设计意图：`.env` 没填密钥。填完 `docker compose up -d` 即可 |
| 页面能开但接口全 502 | 后端没起来或没健康：`docker compose logs backend`；确认 `DB_HOST=mysql`、MySQL 健康检查通过 |
| 刷新 `/admin/xxx` 返回 404 | nginx 的 `try_files ... /index.html` 没生效——确认用的是 `deploy/nginx.conf`（本轮新增），不要用默认站点配置 |
| 上传成功但图片 404 | 上传目录不一致：容器里必须是 `UPLOAD_DIR=/data/files`，且 `./files` 已挂载 |
| 数据库连不上 | 容器间用服务名 `mysql`；本机验证连宿主 MySQL 才用 `host.docker.internal` |
| 端口 80/443 被占 | 服务器上已有 nginx/apache：`systemctl stop nginx` 或改 `.env` 里的 `HTTP_PORT`/`HTTPS_PORT` |
| 本机已有同名容器 `volunteer-mysql` | `docker rm -f volunteer-mysql` 后再 `docker compose up -d`（数据在 `volunteer-system_mysql_data` 卷里，不会丢） |
| 想临时关掉接口文档 | `.env` 里 `SWAGGER_ENABLED=false`，`docker compose up -d backend` |

---

## 7. 本机验证这套编排（不碰公网）

```bash
cp .env.example .env
# 关键：连宿主机上已有的 MySQL 容器
sed -i '' 's|^DB_HOST=.*|DB_HOST=host.docker.internal|' .env     # Linux 用 sed -i
sed -i '' "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')|" .env
printf 'DB_PASSWORD=123456\n' >> .env
mkdir -p deploy/certs && openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
  -keyout deploy/certs/privkey.pem -out deploy/certs/fullchain.pem -subj "/CN=localhost"

docker compose up -d --no-deps --build backend frontend   # 只起这两个，复用本机数据库
curl -kI https://localhost/                        # 前端
curl -k  https://localhost/api/auth/login -X POST -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"x"}'      # 反代是否通（返回业务错误即通）
docker compose down
```

> `--no-deps` 是必需的：compose 默认会连带启动 `mysql` 服务，
> 而本机已有同名容器 `volunteer-mysql`，会报 `container name is already in use`。
>
> 顺手可以验一下"缺密钥不许启动"这条硬约束（应当以非 0 退出）：
>
> ```bash
> docker run --rm -e DB_PASSWORD=x volunteer-system-backend; echo "退出码=$?"
> # Caused by: java.lang.IllegalArgumentException: Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}"
> # 退出码=1
> ```

---

## 8. 上线前仍需知道的两件事

1. **封禁/降权不是即时生效的**：JWT 只验签，不查 `sys_user.status`，token 到期前一直可用
   （演示环境已把有效期调到 12 小时）。要立刻踢人只能改 `JWT_SECRET` 让所有 token 失效。
2. **开放注册 + 无限流**：公网演示可能被脚本刷数据、刷磁盘（上传已从 10MB 收到 2MB）。
   演示期建议关注册，或定期跑 `scripts/reset-demo-data.sh` 重置。

其余已修与未修问题清单见 `docs/问题与修复档案.md`（本地文档）与《面试讲解指南》第 6.2 节。

---

## 9. 用 GitHub Codespaces 做临时演示（免费、自带 HTTPS）

**适合**：下周就要面试、只想当场给一个公网链接。不用买服务器、不用域名，10 分钟拿到
`https://xxx-5173.app.github.dev` —— 外层本身就是 HTTPS，所以**扫码签到（摄像头）也能用**。
**不适合**：长期挂着。Codespaces 闲置会自动停，免费额度以 GitHub 计费页为准（学生账号有额外额度）。

> ⚠️ 这条路线**不要用 nginx 那套 compose**：`deploy/nginx.conf` 里的 301 是按 Host 跳转的，
> 和转发域名对不上。用 Vite dev server + 它自带的 `/api` 代理这条同源路径最省事。

```bash
# ① 装并启动 MySQL（Codespaces 里没有 docker daemon，用不到 compose；lsof 给 dev.sh 的端口检查用）
sudo apt-get update && sudo apt-get install -y mysql-server lsof
sudo service mysql start
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';"
sudo mysql -e "CREATE DATABASE volunteer_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

# ② 建防线 + 演示数据（迁移脚本按日期顺序执行）
export DB_PASSWORD=123456
for f in sql/*.sql; do sudo mysql -uroot -p"$DB_PASSWORD" volunteer_db < "$f"; done
bash scripts/reset-demo-data.sh --yes

# ③ 一条命令起后端 8081 + 前端 5173
#    VITE_HTTPS=false 让前端走 http：外层 GitHub 已经终结了 TLS，
#    内层再套 Vite 的自签证书会握手失败（本地开发不要加这个变量）
VITE_HTTPS=false ./scripts/dev.sh
```

④ 打开「端口」面板，把 **5173** 的可见性改成 **Public**，把给出的
`https://…-5173.app.github.dev` 链接发出去即可。**只需暴露 5173**：前端通过 `/api`、`/files`
代理访问 8081，同源，不会遇到跨域。

⑤ 演示结束直接关掉 Codespace 就行。下次重开时 MySQL 数据会重建，重跑 ①~② 即可。

> `dev.sh` 里的 docker 检查会提示「未检测到 docker」，忽略即可 —— 它只是不去启动 MySQL 容器。
> 想验证 http 模式确实生效：`curl -s -o /dev/null -w '%{http_code}' http://localhost:5173/` 应返回 200，
> 而 `https://localhost:5173/` 连不上（证书已关）。

---

## 10. 用 GitHub Actions + GHCR 自动构建与更新镜像

仓库里已内置两个工作流（`.github/workflows/`），**公开仓库不消耗 Actions 额度**：

| 工作流 | 触发 | 做什么 |
|---|---|---|
| `ci.yml` | push / PR | 起一个真实 MySQL 8 服务容器 → 执行 `sql/*.sql` 迁移 → `./mvnw clean test`（125 例，含真库并发回归）→ 前端 `npm ci && npm run build` → 最后验证两个 Dockerfile 都能构建 |
| `cd.yml` | push 到 main / 手动 | 构建两个镜像并推送到 GHCR，打 `:latest` 与 `:sha-<commit>` 两个 tag；手动触发时可勾选「同时部署到服务器」 |

> 为什么 CI 里要先跑迁移脚本：`SysExchangeRecordCodeConflictTest` 依赖 `uk_redeem_code`
> 唯一索引、并发相关用例依赖 `uk_user_activity_active` 与 CHECK 约束。少跑一次脚本，
> CI 会直接红给你看 —— 这正是把"部署时容易漏的一步"提前暴露出来。

### 10.1 服务器改成拉镜像（推荐；1GB 机型必用）

```bash
# 在服务器仓库目录的 .env 里加两行：
#   GHCR_PREFIX=ghcr.io/<用户名小写>/<仓库名小写>     ← CI 日志里会打印「镜像前缀」
#   IMAGE_TAG=latest
vi .env

# 仓库若是私有的，先登录（公开仓库可直接拉）
echo <你的PAT> | docker login ghcr.io -u <你的GitHub用户名> --password-stdin

# 每次发版：拉新镜像 + 重启（服务器不构建、也不用传源码）
docker compose -f docker-compose.yml -f deploy/compose.ghcr.yml pull
docker compose -f docker-compose.yml -f deploy/compose.ghcr.yml up -d
```

**回滚**：镜像按提交打了 tag，`IMAGE_TAG=sha-<旧提交> docker compose -f docker-compose.yml -f deploy/compose.ghcr.yml up -d` 即可回到那一版。

### 10.2 一键部署（可选，需先配 secrets）

`cd.yml` 的 deploy job 会 SSH 到服务器执行上面那两条命令。启用前在
仓库 **Settings → Secrets and variables → Actions** 添加：

| Secret | 说明 |
|---|---|
| `DEPLOY_HOST` | 服务器 IP 或域名 |
| `DEPLOY_USER` | SSH 用户（如 `root`） |
| `DEPLOY_SSH_KEY` | 私钥全文：`ssh-keygen -t ed25519 -f deploy_key`，公钥追加到服务器 `~/.ssh/authorized_keys` |
| `DEPLOY_PATH` | 仓库在服务器上的路径，例如 `/opt/Volunteer-System` |

配好后：**Actions → CD → Run workflow → 勾选「同时部署到服务器」**。
没配 secrets 也不会影响 push 触发的镜像构建（deploy job 不会运行）。
