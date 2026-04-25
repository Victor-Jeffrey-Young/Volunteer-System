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
 * JWT 拦截器：拦截需要登录的接口，验证 Token 有效性
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头获取 Token (约定前端放在 Authorization)
        String token = request.getHeader("Authorization");

        // 2. 校验 Token
        if (token != null && !token.isEmpty()) {
            Claims claims = jwtUtils.parseToken(token);
            if (claims != null) {
                // Token 有效，允许通过
                request.setAttribute("userId", claims.get("userId"));
                request.setAttribute("role", claims.get("role"));
                return true;
            }
        }

        // 3. 校验失败，返回 JSON 格式的错误提示
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401

        Result<Object> result = Result.error(401, "登录已过期或未登录，请重新登录");
        
        PrintWriter out = response.getWriter();
        out.write(new ObjectMapper().writeValueAsString(result));
        out.flush();
        out.close();

        return false;
    }
}
