package com.volunteer.system.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记「仅管理员可访问」的接口。
 *
 * 由 {@link com.volunteer.system.config.AdminInterceptor} 在请求进入 Controller 之前统一校验，
 * 角色一律取自 JwtInterceptor 写入 request 作用域的 JWT 声明，绝不读取客户端请求头。
 *
 * 为什么不继续在每个方法里手写 if：
 *   授权规则写在方法体里意味着「新增一个管理接口就多一次遗漏的机会」——
 *   项目里曾经有 7 个管理接口完全没有校验。把规则声明在方法上，
 *   由一处拦截器统一执行，漏写注解在代码评审时一眼可见，而不是藏在几百行业务逻辑中间。
 *
 * 用法：
 *   {@code @RequiresAdmin} 加在方法上，或加在 Controller 类上（该类的全部接口都要求管理员）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAdmin {
}
