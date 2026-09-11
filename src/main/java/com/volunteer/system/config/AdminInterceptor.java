package com.volunteer.system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.system.common.RequiresAdmin;
import com.volunteer.system.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

/**
 * 管理员接口授权拦截器：集中执行 {@link RequiresAdmin} 声明的权限规则。
 *
 * 设计要点：
 *  1. 角色来源唯一 —— 只读 JwtInterceptor 写入 request 作用域的 "role"，
 *     该值来自服务端验签后的 JWT 声明，客户端无法伪造。
 *  2. 规则声明化 —— 权限要求写在方法/类的注解上，拦截器统一裁决，
 *     避免「授权逻辑散落在 23 个方法体里、新增接口忘记加校验」。
 *  3. 执行顺序 —— 必须注册在 JwtInterceptor 之后：先认证（你是谁），再授权（你能不能做）。
 *
 * 与 JwtInterceptor 的分工：前者回答「是否登录」，后者回答「是否有权限」。
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // 静态资源等非 Controller 方法，直接放行
            return true;
        }

        // 方法级注解优先，其次看类级注解
        boolean adminOnly = handlerMethod.getMethodAnnotation(RequiresAdmin.class) != null
                || handlerMethod.getBeanType().getAnnotation(RequiresAdmin.class) != null;
        if (!adminOnly) {
            return true;
        }

        if ("ADMIN".equals(request.getAttribute("role"))) {
            return true;   // 放行
        }

        writeForbidden(response);
        return false;
    }

    /** 以统一的 Result 结构返回 403，前端拦截器可直接取 msg 提示用户 */
    private void writeForbidden(HttpServletResponse response) throws Exception {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        Result<Object> result = Result.error(403, "权限不足，该操作仅管理员可执行");
        PrintWriter out = response.getWriter();
        out.write(new ObjectMapper().writeValueAsString(result));
        out.flush();
    }
}
