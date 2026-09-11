package com.volunteer.system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.system.common.Result;
import com.volunteer.system.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

/**
 * JWT 拦截器：拦截需要登录的接口，验证 Token 有效性。
 *
 * 运行机制：
 *  请求进入 Controller 之前，若 URL 匹配拦截器的拦截规则，
 *  会先执行 preHandle()。只有返回 true（Token 有效）才会放行到
 *  Controller；返回 false 则直接以 401 响应结束请求。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 请求处理前执行：校验 Header 中的 Token。
     *
     * @return true 表示放行（Token 有效）；false 表示拦截（响应 401）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头获取 Token (约定前端放在 Authorization)
        //    前端调用受保护接口时须携带: Authorization: Bearer <token>
        String token = request.getHeader("Authorization");

        // 2. 校验 Token
        if (token != null && !token.isEmpty()) {
            // parseToken 内部会：(1) 去掉 "Bearer " 前缀
            // (2) 用密钥验签 (3) 校验是否过期；任一失败返回 null
            Claims claims = jwtUtils.parseToken(token);
            if (claims != null) {
                // Token 有效，允许通过
                // 将 Token 中的 userId、role 写入请求属性，
                // 后续 Controller 可通过 @RequestAttribute 直接获取，无需重复解析。
                // 注意：JJWT 解析出的数字可能是 Integer，统一转为 Long/String，
                // 保证 @RequestAttribute Long userId 的入参类型安全。
                Object rawUserId = claims.get("userId");
                if (rawUserId != null) {
                    request.setAttribute("userId", Long.valueOf(String.valueOf(rawUserId)));
                }
                request.setAttribute("role", String.valueOf(claims.get("role")));
                return true;   // 放行，进入 Controller
            }
        }

        // 3. 校验失败（无 Token / Token 无效 / 已过期），返回 JSON 格式的错误提示
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 状态码 401 未授权

        // 返回统一的 Result 错误结构，方便前端统一处理
        Result<Object> result = Result.error(401, "登录已过期或未登录，请重新登录");

        PrintWriter out = response.getWriter();
        out.write(new ObjectMapper().writeValueAsString(result));
        out.flush();
        out.close();

        return false;
    }
}
