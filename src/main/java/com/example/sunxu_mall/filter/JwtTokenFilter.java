package com.example.sunxu_mall.filter;

import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.helper.TokenHelper;
import com.example.sunxu_mall.util.NoLoginMap;
import com.example.sunxu_mall.util.SpringBeanUtil;
import com.example.sunxu_mall.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/25 20:53
 * @description
 */


@Slf4j
public class JwtTokenFilter extends GenericFilterBean {

    public final static String FILTER_ERROR = "filterError";
    public final static String FILTER_ERROR_PATH = "/throw-error";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestURI = httpServletRequest.getRequestURI();
        
        // 如果是不需要登录的路径，直接放行
        if (!NoLoginMap.notExist(requestURI)) {
            filterChain.doFilter(httpServletRequest, servletResponse);
            return;
        }

        String token = TokenUtil.getTokenForAuthorization(httpServletRequest);
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        // 如果没有token，返回token缺失错误
        if (Objects.isNull(token)) {
            handleException(httpServletRequest, httpServletResponse,
                    new BusinessException(ErrorCode.MISSING_TOKEN.getCode(), ErrorCode.MISSING_TOKEN.getMessage()));
            return;
        }

        TokenHelper tokenHelper = SpringBeanUtil.getBean("tokenHelper");

        if (Objects.isNull(tokenHelper)) {
            log.warn("tokenHelper bean not exist");
            handleException(httpServletRequest, httpServletResponse,
                    new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "Token helper not available"));
            return;
        }

        try {
            // 从 token中获取用户名
            String username = tokenHelper.getUsernameFromToken(token);
            log.info("get user name from token : {}", username);
            
            // 从用户名获取用户详情
            UserDetails userDetails = tokenHelper.getUserDetailsFromUsername(username);
            if (Objects.isNull(userDetails)) {
                handleException(httpServletRequest, httpServletResponse,
                        new BusinessException(ErrorCode.INVALID_TOKEN.getCode(), "Invalid token: user not found"));
                return;
            }
            
            // 设置认证信息到 SecurityContextHolder
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 继续执行后续过滤器
            filterChain.doFilter(httpServletRequest, servletResponse);
        } catch (BusinessException e) {
            // 处理业务异常
            handleException(httpServletRequest, httpServletResponse, e);
        } catch (Exception e) {
            // 处理其他异常
            log.error("Token validation error: {}", e.getMessage(), e);
            handleException(httpServletRequest, httpServletResponse,
                    new BusinessException(ErrorCode.INVALID_TOKEN.getCode(), "Invalid token"));
        }
    }

    private void handleException(HttpServletRequest request,
                                 HttpServletResponse response,
                                 BusinessException e) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + e.getCode() + "\",\"message\":\"" + e.getMessage() + "\"}");
    }
}
