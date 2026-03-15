# 🌟 基于 Web 架构的社区服务管理系统 (CVMS)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue.js-3.x-blue.svg)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-orange.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-Private-red.svg)](https://choosealicense.com/)

## 📖 项目简介
本项目是一套专为现代化社区设计的**志愿服务数字化管理平台**。系统不仅涵盖了基础的活动发布与报名流程，更深度集成了**激励机制（双轨制积分）**、**考勤闭环（O2O 双重扫码打卡）**与**决策辅助（ECharts 数据大屏）**。

系统采用前后端分离架构，通过 **响应式设计** 完美兼容 PC 端管理后台与移动端志愿者操作界面。

## 🚀 核心功能亮点

### 1. 业务闭环与状态机
*   **全生命周期流转**：从活动发布、报名审核、进行中监控到结算归档。
*   **双重时间戳打卡**：支持“签到”与“签退”二次打卡，自动核算服务时长，杜绝虚假工时。

### 2. 精益求精的激励体系
*   **双轨制积分模型**：独立核算“累计荣誉积分”（决定星级段位）与“可用消费积分”（用于商城兑换）。
*   **个人荣誉中心**：基于 Canvas 技术的一键生成专属荣誉证书下载，支持积分排行榜（荣誉殿堂）。

### 3. 高性能与高安全性
*   **并发冲突控制**：利用数据库 **悲观锁 (FOR UPDATE)** 与 Spring 事务控制，确保商城兑换与报名名额不产生“超卖”。
*   **权限分级 (RBAC)**：基于 JWT 令牌的无状态认证，严格区分管理员、志愿者与居民的视图及接口权限。
*   **数据脱敏**：后端接口自动过滤敏感字段（如密码哈希），前端字段级越权更新拦截。

### 4. 数据可视化大屏
*   **多维数据聚合**：利用 SQL 聚合函数实时统计志愿者画像、活动趋势与服务排行，通过 ECharts 动态渲染。

## 🛠️ 技术栈
### 后端 (Backend)
- **核心框架**: Spring Boot 3.2.5 (LTS)
- **持久层**: MyBatis-Plus 3.5.5 (支持物理分页与 Lambda 条件构造)
- **数据库**: MySQL 8.0 (InnoDB)
- **安全认证**: JWT + 自定义拦截器
- **文档工具**: Knife4j (OpenAPI 3)

### 前端 (Frontend)
- **核心框架**: Vue 3.4 + Vite 5
- **UI 组件库**: Element Plus (响应式适配)
- **可视化**: ECharts 5
- **关键库**: html5-qrcode (摄像头扫码), html2canvas (证书生成)

## 📁 目录导航
```text
volunteer-system/
├── src/main/java/.../controller  # RESTful API 接口层
├── src/main/java/.../service     # 核心业务逻辑 (含状态机与锁)
├── src/main/java/.../entity      # 数据库实体与 Schema 注解
├── volunteer-web/                # 前端工程目录
│   ├── src/layout                # 响应式布局组件
│   ├── src/utils                 # Axios 封装与积分算法
│   └── src/views                 # 业务视图组件
├── docs/                         # ADR、API 及设计文档
└── files/                        # 本地静态资源存储
```
---
## 🛠️ 快速开始

### 1. 环境依赖
- JDK 17+
- Node.js 18+
- MySQL 8.0

### 2. 后端部署
1. 修改 `src/main/resources/application.yml` 中的数据库配置。
2. 运行 `mvn clean package` 打包。
3. 执行 `java -jar volunteer-system.jar` 启动。

### 3. 前端部署
```bash
cd volunteer-web
npm install
npm run dev
```

## 🛡️ 维护者备注 (ADR)
*   **关于缓存**：针对 Chrome 激进的缓存策略，所有 GET 请求均注入了随机时间戳参数 `_t` 以强制刷新。
*   **关于 HTTPS**：为在移动端调用摄像头，本地开发环境建议配合 `@vitejs/plugin-basic-ssl` 使用 HTTPS 访问。
*   **关于图片**：系统采用本地文件映射模式，上传的图片存储在根目录 `/files` 下。

---
© 2026 VictorYoung | 基于 MIT 协议开源学习

