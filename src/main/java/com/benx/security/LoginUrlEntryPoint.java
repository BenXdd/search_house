package com.benx.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


/**
 * 基于角色的登录入口控制器
 *  (没有权限时,访问需要权限的接口会跳转到security默认的login页面)
 */
public class LoginUrlEntryPoint extends LoginUrlAuthenticationEntryPoint {

    private PathMatcher pathMatcher = new AntPathMatcher();

    private final Map<String, String> authEntryPointMap;

    public LoginUrlEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
        authEntryPointMap = new HashMap<>();

        //管理员登录入口映射
        authEntryPointMap.put("/admin/**", "/admin/login");
        //普通用户登录入口映射
        authEntryPointMap.put("/user/**","/user/login");
    }

    /**
     * control+o
     * 根据请求跳转到指定的页面, 父类是默认使用 fromLoginUrl
     */
    @Override
    protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        //1.接受原先要跳转到的地址
        String uri = request.getRequestURI().replaceAll(request.getContextPath(),"");
        //2.借助spring的 PathMatcher
        for (Map.Entry<String, String> authEntry : this.authEntryPointMap.entrySet()) {
            if (this.pathMatcher.match(authEntry.getKey(),uri)){
                return authEntry.getValue();
            }
        }
        return super.determineUrlToUseForThisRequest(request, response, exception);

    }
}
