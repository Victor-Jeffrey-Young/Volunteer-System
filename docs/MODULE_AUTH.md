# 📖 模块维护手册：认证与鉴权 (Auth Module)

## 1. 模块定位
负责整个系统的安全门户。处理用户的登录验证、新志愿者注册、Token 签发以及全局路由拦截的起始逻辑。

## 2. 核心链路 (Flow)
1. **视图层**: `src/views/Login.vue` 提供左右分栏的响应式交互界面。
2. **请求层**: `src/utils/request.js` ( Axios拦截器 ) 负责携带后续请求的 Token。
3. **接口层**: `AuthController.java` 接收表单参数。
4. **服务层**: `SysUserServiceImpl.login()` 执行核心查库比对及状态拦截。

## 3. 数据流转说明 (Data Flow)
登录成功后，后端返回的数据结构将被存储至前端 `localStorage` 中。
*   **必须存储项**：`token` (鉴权钥匙), `role` (菜单渲染依据), `userId` (后续业务 API 必传参数)。
*   **辅助存储项**：`realName`, `username` 等，用于在请求到最新用户信息前，作为 UI 的兜底(Fallback)占位显示。

## 4. 维护与避坑指南 (Gotchas)
*   **路由死锁问题**：在 `router/index.js` 的 `beforeEach` 守卫中，**必须**将 `to.path === '/login'` 设为最高优先级的放行条件，否则退出登录时会引发死循环。
*   **Knife4j 调试**：测试其他需授权接口前，必须在本模块的 `login` 接口获取 Token，并在 `doc.html` 的“全局参数设置”中配置 `Authorization` 请求头。
*   **TODO 2.0 升级计划**：
    1.  **密码加密**：当前数据库采用明文存储密码，存在安全隐患。V2.0 需引入 Spring Security 的 `BCryptPasswordEncoder` 进行哈希加盐处理。
    2.  **JWT 替换 UUID**：目前使用的 `UUID` 只是模拟 Token。后续需引入 `jjwt` 库，将 `userId` 和 `role` 压入 JWT 的 Payload (载荷) 中，实现真正的无状态解析。
    3.  **防刷机制**：建议在登录接口增加滑动验证码或图片验证码组件，防止暴力破解。