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
│   │   └── GlobalExceptionHandler.java     # 全局异常处理
│   ├── config/                             # 配置层
│   │   ├── CorsConfig.java                 # 跨域配置
│   │   ├── WebConfig.java                  # 拦截器 & 静态资源
│   │   ├── JwtInterceptor.java             # JWT 认证拦截
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
- JWT Token 无状态认证，请求拦截器自动注入 `Authorization` 头
- 三角色 RBAC：`ADMIN`（管理员）、`VOLUNTEER`（志愿者）、`RESIDENT`（居民）
- 前端路由守卫严格校验角色，越权自动踢回对应首页

### 📋 志愿活动管理
- 管理员发布活动（标题、地点、时间、人数上限、技能要求、奖励工时）
- 活动生命周期：招募中 → 进行中（可打卡）→ 已结束 → 已取消
- 志愿者浏览活动列表、筛选分类、报名参加

### ✅ 报名审批 & 打卡流转
- 报名状态机：待审 → 通过(待签到) → 已签到(进行中) → 已签退(待结算) → 已完结(已发工时)
- 管理员审核报名（通过/拒绝 + 批注），可查看志愿者技能匹配度
- 扫码签到/签退机制
- 结算后自动累加工时与积分

### 🛒 积分商城
- 管理员上架商品（名称、图片、所需积分、库存）
- 志愿者积分兑换，自动扣减 `currentPoints`，库存悲观锁防超卖
- 生成核销验证码，管理员线下扫码核销

### 💝 微心愿墙
- 居民发布心愿（代办/物资/咨询），管理员审核
- 志愿者浏览公开展示的心愿并认领
- 心愿状态流：待审核 → 展示中 → 办理中 → 已标记完成 → 已达成 → 已获赞
- 达成后自动发放积分与工时

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

### 2. 配置数据库连接

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/volunteer_db?serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf-8
    username: root
    password: 你的密码
```

### 3. 启动后端

```bash
./mvnw spring-boot:run
```

后端默认运行在 `http://localhost:8081`，API 文档访问 `http://localhost:8081/doc.html`。

### 4. 启动前端

```bash
cd volunteer-web
npm install
npm run dev
```

前端默认运行在 `https://localhost:5173`，通过 Vite 代理转发 `/api` 请求到后端 `8081`。

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
- **文件存储**：上传文件保存在项目根目录 `files/` 下
- **JWT**：Token 通过 `Authorization` 请求头传递，拦截器自动校验
- **跨域**：`CorsConfig` 允许所有来源，开发/生产均可
- **前端代理**：Vite `proxy` 将 `/api` 和 `/files` 转发至 `http://127.0.0.1:8081`

---

## 许可证

本项目仅供学习与内部使用。
