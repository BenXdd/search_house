
package com.benx.security;

import com.benx.entity.User;
import com.benx.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class AuthProvider implements AuthenticationProvider {
    @Autowired
    private IUserService userService;

    private final Md5PasswordEncoder passwordEncoder = new Md5PasswordEncoder();

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userName = authentication.getName();
        String inputPassword = (String)authentication.getCredentials();
        User user = userService.findUserByName(userName);
        if (user == null){
            throw new AuthenticationCredentialsNotFoundException("authError");
        }

        //user.getId() 通过userid进行加盐  密码通过MD5加密
        if (this.passwordEncoder.isPasswordValid(user.getPassword(), inputPassword,user.getId())){
            return new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
        }
        throw new BadCredentialsException("authError password erroe");


    }



    @Override
    public boolean supports(Class<?> authentication) {
        return true; //支持所有的认证类
    }
}
