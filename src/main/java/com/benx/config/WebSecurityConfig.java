package com.benx.config;


import com.benx.security.AuthProvider;
        import com.benx.security.LoginAuthFailHandler;
        import com.benx.security.LoginUrlEntryPoint;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.context.annotation.Bean;
        import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
        import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
        import org.springframework.security.config.annotation.web.builders.HttpSecurity;
        import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
        import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;



@EnableWebSecurity
@EnableGlobalMethodSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter{

    /**
     * 复写HttpSecurity configure方法
     *
     * HTTP权限控制
     *
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 资源访问权限
        http.authorizeRequests()
                .antMatchers("/admin/login").permitAll()    //管理员登录入口
                .antMatchers("/static/**").permitAll()      //静态资源
                .antMatchers("/admin/**").hasRole("ADMIN")  //admin用户登录入口
                .antMatchers("/user/login").permitAll()     //用户登录入口
                .antMatchers("/user/**").hasAnyRole("ADMINM","USER")
                .antMatchers("/api/user/**").hasAnyRole("ADMIN","USER")
                .and()//结尾
                .formLogin()
                .loginProcessingUrl("/login")  //配置角色登录处理入口  自动匹配到首页index
                .failureHandler(loginAuthFailHandler())  //登录验证失败处理器
                .and()
                .logout()
                .logoutUrl("/logout")      //处理的url还是用原生的处理
                .logoutSuccessUrl("/logout/page") //登出成功显示的页面
                .deleteCookies("JSESSIONID")     //删除cookie中的jsession id
                .invalidateHttpSession(true)     //使我们的会话session失效
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(loginUrlEntryPoint())  //需要注入我们自己写的loginUrlEntryPoint  这里我们使用bean的方式注入
                .accessDeniedPage("/403");     //无权限访问 返回页面

        http.csrf().disable(); //关闭csrf防御策略
        http.headers().frameOptions().sameOrigin();//使用h-ui adminui框架是ifream开发的,则需要使用同源策略

    }

    /**
     * 自定义认证策略
     * 在我们enableWebSecurity下  只能有一个AuthenticationManagerBuilder实例的注入,否则会有未知
     * 的结果发生
     */
    @Autowired
    public void configGlobal(AuthenticationManagerBuilder auth) throws Exception {
        //配置内存 用户名&密码
        //auth.inMemoryAuthentication().withUser("admin").password("admin")
        //        .roles("ADMIN").and();
        auth.authenticationProvider(authProvider()).eraseCredentials(true); //擦除密码
    }

    /**
     * 传入我们自己写的AuthProvider
     * @return
     */
    @Bean
    public AuthProvider authProvider(){
        return new AuthProvider();
    }

    /**
     * 默认走用户的登录入口
     *  在user 或者 admin 登录界面时,登录失败会跳转到默认配置的登录页面
     * @return
     */
    @Bean
    public LoginUrlEntryPoint loginUrlEntryPoint(){
        return new LoginUrlEntryPoint("/user/login");
    }

    @Bean
    public LoginAuthFailHandler loginAuthFailHandler(){
        return new LoginAuthFailHandler(loginUrlEntryPoint());
    }
}
