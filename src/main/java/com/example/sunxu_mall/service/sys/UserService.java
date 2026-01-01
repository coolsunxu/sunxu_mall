package com.example.sunxu_mall.service.sys;


import cn.hutool.core.util.IdUtil;
import com.example.sunxu_mall.entity.auth.AuthUserEntity;
import com.example.sunxu_mall.entity.auth.CaptchaEntity;
import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.entity.auth.TokenEntity;
import com.example.sunxu_mall.entity.sys.web.UserWebEntity;
import com.example.sunxu_mall.event.UserLoginEvent;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.helper.TokenHelper;
import com.example.sunxu_mall.mapper.sys.UserWebEntityMapper;
import com.example.sunxu_mall.util.HttpUtil;
import com.example.sunxu_mall.util.PasswordUtil;
import com.example.sunxu_mall.util.RedisUtil;
import com.example.sunxu_mall.util.TokenUtil;
import com.wf.captcha.SpecCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.sunxu_mall.errorcode.ErrorCode.*;
import static com.example.sunxu_mall.util.CaptchaKeyUtil.getCaptchaKey;

@Slf4j
@Service
public class UserService {
    private final UserWebEntityMapper userMapper;
    private final RedisUtil redisUtil;
    private final PasswordUtil passwordUtil;
    private final AuthenticationManager authenticationManager;
    private final TokenHelper tokenHelper;
    private final UserDetailsService userDetailsService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${mall.mgt.tokenExpireTimeInRecord}")
    private int tokenExpireTimeInRecord;

    @Value("${mall.mgt.captchaExpireSecond}")
    private int captchaExpireSecond;

    @Value("${mall.mgt.captcha.enabled:true}")
    private boolean captchaEnabled;


    public UserService(
            UserWebEntityMapper userMapper,
            RedisUtil redisUtil,
            PasswordUtil passwordUtil,
            AuthenticationManager authenticationManager,
            TokenHelper tokenHelper,
            UserDetailsService userDetailsService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userMapper = userMapper;
        this.redisUtil = redisUtil;
        this.passwordUtil = passwordUtil;
        this.authenticationManager = authenticationManager;
        this.tokenHelper = tokenHelper;
        this.userDetailsService = userDetailsService;
        this.eventPublisher = eventPublisher;
    }

    public JwtUserEntity getUserInfo() {
        String currentUsername = tokenHelper.getCurrentUsername();
        UserDetails userDetails = userDetailsService.loadUserByUsername(currentUsername);
        if (userDetails instanceof JwtUserEntity) {
            return (JwtUserEntity) userDetails;
        }
        throw new BusinessException(INTERNAL_SERVER_ERROR.getCode(), "Failed to load user info");
    }

    /**
     * User logout
     *
     * @param request 请求
     */
    public void logout(HttpServletRequest request) {
        log.info("logout begin");

        String token = TokenUtil.getTokenForAuthorization(request);
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(UNAUTHORIZED.getCode(), UNAUTHORIZED.getMessage());
        }

        tokenHelper.delToken(token);
        log.info("logout end");
    }

    /**
     * User login
     *
     * @param authUserEntity User input information
     */
    public TokenEntity login(AuthUserEntity authUserEntity) {
        validateLoginParams(authUserEntity);
        validateCaptchaIfEnabled(authUserEntity);

        try {
            String decodePassword = passwordUtil.decodeRsaPassword(authUserEntity);
            Authentication authentication = authenticate(authUserEntity.getUsername(), decodePassword);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            JwtUserEntity jwtUserEntity = getJwtPrincipal(authentication);
            UserWebEntity userEntity = loadUser(jwtUserEntity.getUsername());

            String token = tokenHelper.generateToken(jwtUserEntity);
            if (StringUtils.isBlank(token)) {
                throw new BusinessException(OPERATION_FAILED.getCode(), "Token generation failed");
            }

            String clientIp = resolveClientIpSafely();
            log.info("[login] -> Login success, user {}, ip {}", userEntity.getUserName(), clientIp);

            eventPublisher.publishEvent(new UserLoginEvent(
                    this,
                    userEntity.getId(),
                    userEntity.getUserName(),
                    clientIp,
                    LocalDateTime.now(),
                    userEntity.getLastLoginCity()
            ));

            List<String> roles = jwtUserEntity.getAuthorities().stream()
                    .map(SimpleGrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return new TokenEntity(jwtUserEntity.getUsername(), token, roles, tokenExpireTimeInRecord);
        } catch (AuthenticationException e) {
            log.warn("[login] -> Authentication failed, username {}", authUserEntity.getUsername());
            throw new BusinessException(UNAUTHORIZED.getCode(), UNAUTHORIZED.getMessage());
        } catch (BusinessException e) {
            log.warn("[login] -> Login failed, username {}, code {}", authUserEntity.getUsername(), e.getCode(), e);
            throw e;
        } catch (Exception e) {
            log.error("[login] -> Login failed, username {}", authUserEntity.getUsername(), e);
            throw new BusinessException(INTERNAL_SERVER_ERROR.getCode(), INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    public CaptchaEntity getCode() {
        SpecCaptcha captcha = new SpecCaptcha(111, 36, 4);
        // 设置字符类型：数字和字母混合
        captcha.setCharType(com.wf.captcha.base.Captcha.TYPE_DEFAULT);
        // 获取验证码文本, 统一大小写
        String result = captcha.text().toLowerCase();
        String uuid = "C" + IdUtil.simpleUUID();
        // 保存验证码到 Redis中
        redisUtil.set(getCaptchaKey(uuid), result, captchaExpireSecond, TimeUnit.SECONDS);
        return new CaptchaEntity(uuid, captcha.toBase64());
    }

    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void updateLoginCity(UserWebEntity userWebEntity) {
        if (Objects.isNull(userWebEntity) || Objects.isNull(userWebEntity.getId())) {
            return;
        }

        Integer version = userMapper.selectVersionById(userWebEntity.getId());
        if (Objects.isNull(version)) {
            return;
        }

        UserWebEntity update = UserWebEntity.builder()
                .id(userWebEntity.getId())
                .lastLoginCity(userWebEntity.getLastLoginCity())
                .lastLoginTime(userWebEntity.getLastLoginTime())
                .version(version)
                .build();

        int rows = userMapper.updateLoginInfoWithVersion(update);
        if (rows == 0) {
            log.warn("Optimistic lock failure for userId {}", userWebEntity.getId());
            throw new OptimisticLockingFailureException("User data has been modified, please retry");
        }
    }

    private void validateLoginParams(AuthUserEntity authUserEntity) {
        if (Objects.isNull(authUserEntity)) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "Login parameters cannot be null");
        }

        if (StringUtils.isBlank(authUserEntity.getUsername())) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "Username cannot be empty");
        }

        if (StringUtils.isBlank(authUserEntity.getPassword())) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "Password cannot be empty");
        }

        if (captchaEnabled) {
            if (StringUtils.isBlank(authUserEntity.getUuid())) {
                throw new BusinessException(PARAMETER_MISSING.getCode(), "Captcha uuid cannot be empty");
            }

            if (StringUtils.isBlank(authUserEntity.getCode())) {
                throw new BusinessException(PARAMETER_MISSING.getCode(), "Captcha code cannot be empty");
            }
        }
    }

    private void validateCaptchaIfEnabled(AuthUserEntity authUserEntity) {
        if (!captchaEnabled) {
            return;
        }

        String redisKey = getCaptchaKey(authUserEntity.getUuid());
        String code = redisUtil.get(redisKey);
        if (StringUtils.isEmpty(code)) {
            throw new BusinessException(CAPTCHA_EXPIRED.getCode(), CAPTCHA_EXPIRED.getMessage());
        }

        String inputCode = authUserEntity.getCode().toLowerCase();
        if (!Objects.equals(code, inputCode)) {
            throw new BusinessException(CAPTCHA_ERROR.getCode(), CAPTCHA_ERROR.getMessage());
        }

        redisUtil.delete(redisKey);
    }

    private Authentication authenticate(String username, String decodePassword) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, decodePassword);
        return authenticationManager.authenticate(authenticationToken);
    }

    private JwtUserEntity getJwtPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtUserEntity) {
            return (JwtUserEntity) principal;
        }
        throw new BusinessException(INTERNAL_SERVER_ERROR.getCode(), "Unsupported principal type");
    }

    private UserWebEntity loadUser(String username) {
        UserWebEntity userEntity = userMapper.findByUserName(username);
        if (Objects.isNull(userEntity)) {
            throw new BusinessException(USER_NOT_EXIST.getCode(), USER_NOT_EXIST.getMessage());
        }
        return userEntity;
    }

    private String resolveClientIpSafely() {
        try {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)) {
                return "unknown";
            }
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            return HttpUtil.getClientIp(request);
        } catch (Exception e) {
            return "unknown";
        }
    }

}
