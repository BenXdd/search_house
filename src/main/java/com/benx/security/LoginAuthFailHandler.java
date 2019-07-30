package com.benx.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录验证失败处理器
 */
public class LoginAuthFailHandler extends SimpleUrlAuthenticationFailureHandler{

    private final LoginUrlEntryPoint urlEntryPoint;

    public LoginAuthFailHandler (LoginUrlEntryPoint urlEntryPoint){
        this.urlEntryPoint = urlEntryPoint;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        //1.获取跳转的url
        String targeturl = urlEntryPoint.determineUrlToUseForThisRequest(request,response,exception);
        //2.修改参数 url+异常信息
        targeturl += "?" +exception.getMessage();
        //3.调用父类方法跳转到我们的url
        super.setDefaultFailureUrl(targeturl);
        //4.调用父类方法处理跳转逻辑
        super.onAuthenticationFailure(request,response,exception);
    }
}
