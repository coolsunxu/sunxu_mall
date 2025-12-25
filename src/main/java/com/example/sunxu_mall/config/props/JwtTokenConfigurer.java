package com.example.sunxu_mall.config.props;

import com.example.sunxu_mall.filter.JwtTokenFilter;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/25 20:52
 * @description
 */

public class JwtTokenConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    @Override
    public void configure(HttpSecurity httpSecurity) {
        JwtTokenFilter jwtTokenFilter = new JwtTokenFilter();
        httpSecurity.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
