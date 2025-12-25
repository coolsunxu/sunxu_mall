package com.example.sunxu_mall.filter;

import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.helper.TokenHelper;
import com.example.sunxu_mall.util.NoLoginMap;
import com.example.sunxu_mall.util.SpringBeanUtil;
import com.example.sunxu_mall.util.TokenUtil;
import org.springframework.http.HttpStatus;
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

public class JwtTokenFilter extends GenericFilterBean {

    public final static String FILTER_ERROR = "filterError";
    public final static String FILTER_ERROR_PATH = "/throw-error";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        if (!NoLoginMap.notExist(httpServletRequest.getRequestURI())) {
            filterChain.doFilter(httpServletRequest, servletResponse);
            return;
        }

        String token = TokenUtil.getTokenForAuthorization(httpServletRequest);

        if (Objects.isNull(token)) {
            if (NoLoginMap.notExist(httpServletRequest.getRequestURI())) {
                handleException((HttpServletRequest) servletRequest,
                        (HttpServletResponse) servletResponse,
                        new BusinessException(HttpStatus.FORBIDDEN.value(), "请先登录"));
            } else {
                filterChain.doFilter(httpServletRequest, servletResponse);
            }
            return;
        }

        TokenHelper tokenHelper = SpringBeanUtil.getBean("tokenHelper");

        if (Objects.nonNull(tokenHelper)) {
            try {
                String username = tokenHelper.getUsernameFromToken(token);
                if (StringUtils.hasLength(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = tokenHelper.getUserDetailsFromUsername(username);
                    if (Objects.nonNull(userDetails)) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
                filterChain.doFilter(httpServletRequest, servletResponse);
            } catch (BusinessException e) {
                handleException((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, e);
            }
        } else {
            filterChain.doFilter(httpServletRequest, servletResponse);
        }
    }

    private void handleException(HttpServletRequest request,
                                 HttpServletResponse response,
                                 BusinessException e) throws ServletException, IOException {
        request.setAttribute(FILTER_ERROR, e);
        request.getRequestDispatcher(FILTER_ERROR_PATH).forward(request, response);
    }
}
