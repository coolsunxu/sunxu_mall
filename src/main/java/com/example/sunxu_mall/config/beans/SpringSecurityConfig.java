package com.example.sunxu_mall.config.beans;


import com.example.sunxu_mall.annotation.NoLogin;
import com.example.sunxu_mall.config.props.JwtTokenConfig;
import com.example.sunxu_mall.service.user.UserDetailsServiceImpl;
import com.example.sunxu_mall.util.NoLoginMap;
import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

/**
 * Spring Security Configuration Class
 * <p>
 * This class configures Spring Security for the application, including:
 * - Authentication manager configuration
 * - Password encoding
 * - URL access rules
 * - Session management
 * - CSRF protection
 * - Exception handling
 *
 * @author sunxu
 * @date 2025/12/24
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SpringSecurityConfig implements ApplicationContextAware {

    private final UserDetailsServiceImpl userDetailsService;

    private ApplicationContext applicationContext;

    /**
     * Constructor injection for UserDetailsServiceImpl
     *
     * @param userDetailsService UserDetailsService implementation for user authentication
     */
    public SpringSecurityConfig(
            UserDetailsServiceImpl userDetailsService,
            ApplicationContext applicationContext
    ) {
        this.userDetailsService = userDetailsService;
        this.applicationContext = applicationContext;
    }

    /**
     * Configure DaoAuthenticationProvider
     * <p>
     * Sets up password encoder and user details service for authentication
     *
     * @return Configured DaoAuthenticationProvider
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    /**
     * Configure AuthenticationManager
     * <p>
     * Creates an AuthenticationManager with the configured authentication providers
     * <p>
     * This method is designed to support multiple authentication providers in the future
     *
     * @return Configured AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        List<AuthenticationProvider> providers = new ArrayList<>();
        // Add the default DAO authentication provider
        providers.add(daoAuthenticationProvider());

        // Additional providers can be added here in the future
        // providers.add(anotherAuthenticationProvider());

        return new ProviderManager(providers);
    }

    /**
     * Configure SecurityFilterChain
     * <p>
     * Sets up HTTP security rules, including:
     * - URL access permissions
     * - Session management
     * - CSRF protection
     * - Exception handling
     * 
     * @param http HttpSecurity to configure
     * @return Configured SecurityFilterChain
     * @throws Exception If configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        initNoLogin(applicationContext);
        return http
                // 启用 CORS
                .cors()
                .and()
                // 禁用 CSRF
                .csrf().disable()
                // 授权异常
                .exceptionHandling()

                .and()
                .headers()
                .frameOptions()
                .disable()

                // 不创建会话
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                .and()
                .authorizeRequests()
                // 静态资源等等
                .antMatchers(
                        HttpMethod.GET,
                        "/*.html",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js"
                ).permitAll()
                .antMatchers(
                        "/ws/**",
                        "/files/**",
                        "/job/**",
                        "/init/**"
                ).permitAll()
                // swagger 文档
                .antMatchers("/swagger-ui.html").permitAll()
                .antMatchers("/swagger-ui/**").permitAll()
                .antMatchers("/swagger-resources/**").permitAll()
                .antMatchers("/v2/api-docs").permitAll()
                .antMatchers("/v2/api-docs/**").permitAll()
                .antMatchers("/webjars/**").permitAll()
                .antMatchers("/*/api-docs").permitAll()
                .antMatchers("/avatar/**").permitAll()
                .antMatchers("/druid/**").permitAll()
                // 放行 OPTIONS请求
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(NoLoginMap.getNoLoginUrlSet().toArray(new String[0])).permitAll()
                // 所有请求都需要认证
                .anyRequest().authenticated()
                .and().apply(new JwtTokenConfig())
                .and()
                .build();
    }

    /**
     * Configure GrantedAuthorityDefaults
     * <p>
     * Removes the "ROLE_" prefix from authority names for simpler role management
     * 
     * @return Configured GrantedAuthorityDefaults
     */
    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults("");
    }

    /**
     * Configure PasswordEncoder
     * <p>
     * Uses BCryptPasswordEncoder with default strength for password encryption
     * 
     * @return Configured PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 10 (default)
        return new BCryptPasswordEncoder();
    }

    private void initNoLogin(ApplicationContext applicationContext) {
        Map<RequestMappingInfo, HandlerMethod> handlerMethodMap = applicationContext.getBean(RequestMappingHandlerMapping.class).getHandlerMethods();
        Set<String> noLoginUrls = new HashSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> infoEntry : handlerMethodMap.entrySet()) {
            HandlerMethod handlerMethod = infoEntry.getValue();
            NoLogin noLogin = handlerMethod.getMethodAnnotation(NoLogin.class);
            if (null != noLogin) {
                noLoginUrls.addAll(infoEntry.getKey().getPatternsCondition().getPatterns());
            }
        }
        NoLoginMap.initSet(noLoginUrls);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
