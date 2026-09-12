# 🫶 社区志愿服务管理系统

基于 **Spring Boot 3 + Vue 3** 的全栈社区志愿服务管理平台，覆盖志愿者、管理员、居民三类角色，实现活动招募、签到打卡、积分商城、微心愿墙等完整业务闭环。

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.x |
| 认证鉴权 | JWT (jjwt) | 0.11.5 |
| API 文档 | Knife4j / OpenAPI 3 | 4.5.0 |
| 前端框架 | Vue 3 (Composition API) | 3.5 |
| 构建工具 | Vite | 7.3 |
| UI 组件库 | Element Plus | 2.13 |
| 样式方案 | Tailwind CSS | 4.2 |
| 状态管理 | Pinia | 3.0 |
| 路由 | Vue Router | 4.6 |
| HTTP 客户端 | Axios | 1.13 |
| 图表 | ECharts | 6.0 |

---

## 项目结构

```
Volunteer-System/
├── src/main/java/com/volunteer/system/
│   ├── VolunteerSystemApplication.java    # 启动入口
│   ├── common/                             # 通用组件
│   │   ├── Result.java                     # 统一响应体
│   │   ├── ServiceException.java           # 业务异常
│   │   ├── RequiresAdmin.java              # 管理员接口声明式注解
│   │   └── GlobalExceptionHandler.java     # 全局异常处理
│   ├── config/                             # 配置层
│   │   ├── CorsConfig.java                 # 跨域配置
│   │   ├── WebConfig.java                  # 拦截器 & 静态资源
│   │   ├── JwtInterceptor.java             # 认证：验签并注入可信身份
│   │   ├── AdminInterceptor.java           # 授权：统一裁决 @RequiresAdmin
│   │   └── MybatisPlusConfig.java          # 分页插件
│   ├── controller/                         # 控制器 (9 个模块)
│   │   ├── AuthController.java             # 登录/注册
│   │   ├── UserController.java             # 用户管理
│   │   ├── ActivityController.java         # 志愿活动
│   │   ├── RegistrationController.java     # 报名与打卡
│   │   ├── ShopController.java             # 积分商城
│   │   ├── WishController.java             # 微心愿
│   │   ├── NoticeController.java           # 公告通知
│   │   ├── DashboardController.java        # 数据看板
│   │   └── FileController.java             # 文件上传
│   ├── entity/                             # 实体类 (7 张核心表)
│   ├── mapper/                             # MyBatis-Plus Mapper
│   ├── service/                            # 业务接口 & 实现
│   ├── task/                               # 定时任务 (排名计算)
│   └── utils/                              # 工具类 (JWT)
├── src/main/resources/
│   └── application.yml                     # 应用配置
├── files/                                  # 静态文件存储
├── volunteer-web/                          # 前端项目
│   ├── src/
│   │   ├── api/modules.js                  # API 接口封装
│   │   ├── utils/request.js                # Axios 拦截器
│   │   ├── stores/user.js                  # Pinia 用户状态
│   │   ├── router/index.js                 # 路由 & RBAC 守卫
│   │   ├── layout/                         # 布局组件 (3 种角色)
│   │   └── views/                          # 页面视图
│   │       ├── Login.vue
│   │       ├── admin/                      # 管理员端 (10 页)
│   │       ├── volunteer/                  # 志愿者端 (7 页)
│   │       └── resident/                   # 居民端 (3 页)
│   ├── vite.config.js                      # Vite 配置 & 代理
│   └── package.json
└── pom.xml                                 # Maven 依赖
```

---

## 核心功能

### 🔐 认证与权限
- JWT Token 无状态认证（HS256），请求拦截器自动注入 `Authorization` 头
- 三角色 RBAC：`ADMIN`（管理员）、`VOLUNTEER`（志愿者）、`RESIDENT`（居民）
- **两层拦截器分工**：`JwtInterceptor` 负责认证（你是谁），`AdminInterceptor` 负责授权（你能不能做）
- 管理接口用 `@RequiresAdmin` 注解声明权限要求，由拦截器统一裁决 —— 授权规则只写一处，不再散落在各方法体里
- 身份（`userId` / `role`）一律取自 JWT 声明，不信任客户端传入的请求头、URL 参数或请求体
- 前端角色不落 localStorage，刷新时由服务端回灌，篡改本地存储无法切换界面或提权
- 密码使用 BCrypt 存储，兼容旧 MD5 数据并在登录时自动升级

### 📋 志愿活动管理
- 管理员发布活动（标题、地点、时间、人数上限、技能要求、奖励工时）
- 活动生命周期：招募中 → 进行中（可打卡）→ 已结束 → 已取消
- 志愿者浏览活动列表、筛选分类、报名参加
- **并发防护**：报名在事务内先对活动行加 `FOR UPDATE` 锁，名额计数不会因并发丢更新

### ✅ 报名审批 & 打卡流转
- 报名状态机：待审 → 通过(待签到) → 已签到(进行中) → 已签退(待结算) → 已完结(已发工时)
- 管理员审核报名（通过/拒绝 + 批注），可查看志愿者技能匹配度
- 扫码签到/签退机制
- 结算后自动累加工时与积分（原子 UPDATE，工时结算对报名行加锁保证幂等）
- **防重复报名三层防线**：活动行锁 → 判重状态集合 → `sys_registration` 唯一索引兜底

### 🛒 积分商城
- 管理员上架商品（名称、图片、所需积分、库存）
- 志愿者积分兑换：商品行悲观锁防超卖，积分扣减用条件 UPDATE 防透支
- 生成随机核销码（`SecureRandom` + 唯一索引），管理员线下扫码核销，一码只能核销一次

### 💝 微心愿墙
- 居民发布心愿（代办/物资/咨询），管理员审核
- 志愿者浏览公开展示的心愿并认领（条件更新，并发下只有一人能认领成功）
- 心愿状态流：待审核 → 展示中 → 办理中 → 已标记完成 → 已达成 → 已获赞
- 归属校验：标记完成仅限认领人、确认与点赞仅限发布人，发布者身份由 JWT 注入
- 达成后自动发放积分与工时（奖励参数有上下限校验）

### 📊 数据看板
- 实时统计：注册人数、活动总量、完成活动数、总服务时长
- 志愿者排行榜（积分榜 / 时长榜），每日凌晨定时刷新
- ECharts 可视化图表

### 📢 公告通知
- 管理员发布公告（重要通知 / 志愿新闻）
- 志愿者首页展示未读公告，支持全部/单条已读标记

### 👤 个人中心
- 个人信息编辑、头像上传、密码修改
- 志愿者展示累计工时、荣誉积分、当前积分、段位等级
- 报名记录、兑换记录、心愿记录查询

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Node.js 18+

### 1. 初始化数据库

创建数据库并导入初始数据：

```sql
CREATE DATABASE IF NOT EXISTS volunteer_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2. 配置数据库连接与密钥

配置项都支持环境变量覆盖，默认值仅面向本地开发（**生产环境必须覆盖 `JWT_SECRET` 与 `DB_PASSWORD`**）：

| 配置项 | 环境变量 | 默认值 |
|---|---|---|
| `spring.datasource.url` | `DB_HOST` / `DB_PORT` / `DB_NAME` | `127.0.0.1:3306/volunteer_db` |
| `spring.datasource.username` | `DB_USERNAME` | `root` |
| `spring.datasource.password` | `DB_PASSWORD` | `123456` |
| `jwt.secret` | `JWT_SECRET` | 内置开发密钥（启动时会打印安全提醒） |
| `jwt.expire-hours` | `JWT_EXPIRE_HOURS` | `24` |

> `jwt.secret` 长度不足 32 字节时应用会**直接启动失败** —— HS256 要求密钥至少 256 位。
> 源码里的密钥一旦泄漏，攻击者就能离线伪造任意角色的 Token，绕过全部鉴权。

### 3. 执行数据库迁移脚本

`sql/` 下的脚本需按日期顺序执行一次：

```bash
mysql -uroot -p volunteer_db < sql/2026-09-09-registration-unique-index.sql   # 防重复报名的唯一索引
mysql -uroot -p volunteer_db < sql/2026-09-12-redeem-code-unique-index.sql   # 核销码唯一索引
mysql -uroot -p volunteer_db < sql/2026-09-13-counter-non-negative-check.sql # 库存/名额/积分不允许为负
```

> ⚠️ 这三个脚本是**并发与安全防线的数据库兜底**，跳过不执行不会报错，但最后一道防线就不存在了。
> 最后一个脚本会加 `CHECK` 约束（需要 MySQL 8.0.16+），执行前请先跑脚本里的第 1 步检查确认没有负值。
> 后续计划引入 Flyway 把它们纳入版本管理（见 `docs/面试讲解指南.md` 第 6 章 R2）。

### 4. 启动后端

```bash
./mvnw spring-boot:run
```

后端默认运行在 `http://localhost:8081`，API 文档访问 `http://localhost:8081/doc.html`。

### 5. 启动前端

```bash
cd volunteer-web
npm install
npm run dev
```

前端默认运行在 `https://localhost:5173`，通过 Vite 代理转发 `/api` 请求到后端 `8081`。

> 本地开发使用 HTTPS 是**必需的**，不是可选项：志愿者的扫码签到依赖浏览器的
> `getUserMedia` 调用摄像头，而它只在安全上下文（HTTPS / localhost）中可用。

### 6. 运行测试

```bash
./mvnw test        # 110 个用例
```

> 其中 `contextLoads` 与 `concurrency/` 下的并发回归测试需要本地 MySQL 可连接。
> 测试覆盖：鉴权与越权回归（用**真实签名的 Token** 走完整拦截器链，而不是把 `JwtUtils` mock 掉）、
> 微心愿状态机与归属校验、并发与原子性、密码迁移、文件上传安全。
>
> `concurrency/` 两个类是真库 + 真多线程（`CountDownLatch` 同时起跑，5 线程）的回归测试，
> 专门锁死这几类竞态：并发报名只落一条、并发取消/驳回只释放一个名额、并发签到签退只生效一次、
> 并发兑换只成交一单、管理端编辑不得覆盖并发期间的报名数与库存。用例自建数据并在结束后清理。

### 7. 用 VSCode 启动（可选，替代 IDEA）

仓库内置了 `.vscode/` 配置与 `scripts/dev.sh`，不依赖 IDEA：

```bash
./scripts/dev.sh            # 一键启动后端 8081 + 前端 5173
./scripts/dev.sh backend    # 只启动后端
./scripts/dev.sh frontend   # 只启动前端
```

脚本会先检查 MySQL 容器（`volunteer-mysql`）、JDK、Node 与端口占用，输出带 `[后端]`/`[前端]`
前缀，日志落在 `$TMPDIR/volunteer-system-dev/*.log`，按 `Ctrl+C` 一次性停掉全部子进程。

在 VSCode 里等价的操作：

| 入口 | 说明 |
|------|------|
| 运行和调试（`F5`）→ `🚀 全栈启动（终端，免扩展）` | 起后端 + 前端，无需安装任何扩展 |
| 运行和调试 → `🚀 全栈启动（Java 断点调试）` | 后端支持 Java 断点，需安装推荐的 Java 扩展包 |
| 终端 → 运行任务 → `全栈：一键启动（任务模式）` | 用 VSCode 任务面板分别管理两个终端 |
| 终端 → 运行任务 → `后端：清理并启动（编译报诡异错误时用）` | 先删 `target/` 再全量重编译，见下方故障排查 |
| 终端 → 运行任务 → `数据库：启动 MySQL 容器` | 本地 MySQL 没起来时先跑这个 |

首次用 VSCode 打开项目时会提示安装推荐扩展（Java 扩展包、Vue Volar、Docker）；
只想用终端模式跑起来的话可以直接忽略。

> **编译报出看不懂的错误时先 `clean` 一次。** 如果 Maven 报的是
> `找不到符号 方法 xxx(java.lang.Object)`、`无法访问BigDecimal / 找不到BigDecimal的类文件`、
> `assertThrows 找不到合适的方法` 这类**和源码对不上**的错误，那不是代码问题：
> IDE 的 Java 插件会把自己的字节码写进 `target/classes`，而 Maven 靠时间戳判断类是否最新，
> 于是直接用了插件产出的残缺 class 文件。执行 `./mvnw clean`（或上面那个 clean 任务）即可恢复。
> 本项目已在 `.vscode/settings.json` 里关闭 `java.autobuild.enabled`，从源头避免这种互相覆盖。

---

## 数据库表结构

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表（管理员/志愿者/居民） |
| `sys_activity` | 志愿活动表 |
| `sys_registration` | 报名与打卡流转表 |
| `sys_goods` | 积分商城商品表 |
| `sys_exchange_record` | 积分兑换流水表 |
| `sys_wish` | 微心愿表 |
| `sys_notice` | 公告通知表 |

---

## API 接口概览

| 模块 | 前缀 | 主要接口 |
|------|------|----------|
| 认证 | `/api/auth` | `POST /login` `POST /register` |
| 用户 | `/api/user` | `GET /page` `GET /info` `PUT /profile` `PUT /password` |
| 活动 | `/api/activity` | `GET /page` `POST /` `PUT /` `DELETE /` |
| 报名 | `/api/reg` | `POST /apply` `GET /my` `PUT /sign` `PUT /sign-out` |
| 商城 | `/api/shop` | `GET /page` `POST /exchange` `PUT /verify` |
| 心愿 | `/api/wish` | `GET /my` `GET /feeds` `POST /` `PUT /claim` |
| 公告 | `/api/notice` | `GET /page` `POST /read/{id}` `POST /read-all` |
| 看板 | `/api/dashboard` | `GET /base` `GET /volunteer/rank` |
| 文件 | `/api/file` | `POST /upload` |

---

## 角色功能矩阵

| 功能 | 管理员 | 志愿者 | 居民 |
|------|:---:|:---:|:---:|
| 用户管理 | ✅ | - | - |
| 活动发布/管理 | ✅ | - | - |
| 报名审核 | ✅ | - | - |
| 商品/核销管理 | ✅ | - | - |
| 心愿审核 | ✅ | - | - |
| 公告发布 | ✅ | - | - |
| 数据看板 | ✅ | - | - |
| 浏览/报名活动 | - | ✅ | - |
| 签到/签退 | - | ✅ | - |
| 积分商城兑换 | - | ✅ | - |
| 认领/达成心愿 | - | ✅ | - |
| 排行榜查看 | - | ✅ | - |
| 发布微心愿 | - | - | ✅ |
| 确认心愿达成 | - | - | ✅ |
| 个人中心 | ✅ | ✅ | ✅ |

---

## 配置说明

- **服务端口**：`server.port` 默认为 `8081`
- **文件存储**：上传文件保存在项目根目录 `files/` 下（目录名走白名单，扩展名走白名单）
- **JWT**：Token 通过 `Authorization` 请求头传递（裸 Token 或 `Bearer ` 前缀都兼容），拦截器自动验签
- **跨域**：`CorsConfig` 目前允许所有来源 —— **自用/演示环境的取舍，生产必须收敛到具体域名**
- **前端代理**：Vite `proxy` 将 `/api` 和 `/files` 转发至 `http://127.0.0.1:8081`

---

## 项目文档

| 文档 | 内容 |
|---|---|
| [`docs/面试讲解指南.md`](docs/面试讲解指南.md) | 项目讲解思路、技术亮点、AI 协作方式、已修/未修缺陷盘点、追问题库 |
| [`docs/越权与并发一致性问题说明报告.md`](docs/越权与并发一致性问题说明报告.md) | 四类安全与并发问题的机制分析、复现代码、修复方案与验证（附录 D 为落地情况复核） |
| [`docs/报名机制与重复报名漏洞-新手讲解版.md`](docs/报名机制与重复报名漏洞-新手讲解版.md) | 报名状态机与重复报名漏洞的通俗讲解 |
| [`docs/系统介绍讲稿-视频版.md`](docs/系统介绍讲稿-视频版.md) | 系统演示视频的分幕讲稿 |

---

## 已知限制

- **Token 无法主动失效**：JWT 24 小时有效，无 `jti`/黑名单，拦截器不查 `sys_user.status` —— 封禁用户或降级管理员在 Token 到期前依然可用
- **DDL 靠手工执行**：`sql/` 脚本尚未纳入 Flyway，换环境部署时最后一道防线可能不存在
- **测试依赖本地 MySQL**：`contextLoads` 需要真实数据库，因此暂时无法接入 CI
- **`/files/**` 不走认证**：静态资源映射不在 `/api/**` 拦截范围内
- **账号注册判重仍是先查后插**：`sys_user.username` 有唯一索引不会产生脏数据，但并发重复注册返回 500 而非友好 409
- **上传测试会污染真实目录**：`FileControllerTest` 直接往 `files/` 写文件，跑测试会留下残留图片；应改为写入 `@TempDir`

---

## 许可证

本项目仅供学习与内部使用。
