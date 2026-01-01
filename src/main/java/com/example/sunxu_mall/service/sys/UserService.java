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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
        return (JwtUserEntity) userDetailsService.loadUserByUsername(currentUsername);
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
        String redisKey = getCaptchaKey(authUserEntity.getUuid());
        String code = redisUtil.get(getCaptchaKey(authUserEntity.getUuid()));

        // 检查验证码是否过期
//        if (StringUtils.isEmpty(code)) {
//            throw new BusinessException(CAPTCHA_EXPIRED.getCode(), CAPTCHA_EXPIRED.getMessage());
//        }
//
//        // 检查验证码是否正确
//        if (!Objects.equals(code, authUserEntity.getCode().toLowerCase())) {
//            log.info("real code {}, provide code {}", code, authUserEntity.getCode());
//            throw new BusinessException(CAPTCHA_ERROR.getCode(), CAPTCHA_ERROR.getMessage());
//        }

        try {
            // 解码密码
            String decodePassword = passwordUtil.decodeRsaPassword(authUserEntity);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(authUserEntity.getUsername(), decodePassword);
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            JwtUserEntity jwtUserEntity = (JwtUserEntity) (authentication.getPrincipal());
            UserWebEntity userEntity = userMapper.findByUserName(jwtUserEntity.getUsername());

            if (Objects.isNull(userEntity)) {
                throw new BusinessException(USER_NOT_EXIST.getCode(), USER_NOT_EXIST.getMessage());
            }

            // 获取客户端 IP地址
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            String clientIp = HttpUtil.getClientIp(request);
            log.info("[login] -> Login success, user {}, ip {}", userEntity.getUserName(), clientIp);

            // 异步处理登录事件（IP查询、日志记录等）
            eventPublisher.publishEvent(new UserLoginEvent(this, String.valueOf(userEntity.getId()), userEntity.getUserName(), clientIp, LocalDateTime.now()));

            // 生成 token 并返回
            String token = tokenHelper.generateToken(jwtUserEntity);
            redisUtil.delete(redisKey);
            List<String> roles = jwtUserEntity.getAuthorities().stream()
                    .map(SimpleGrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return new TokenEntity(jwtUserEntity.getUsername(), token, roles, tokenExpireTimeInRecord);
        } catch (BusinessException e) {
            log.info("[login] -> Login failed:", e);
            throw e;
        } catch (Exception e) {
            log.info("[login] -> Login failed:", e);
            throw new BusinessException(BAD_REQUEST.getCode(), "unknown error");
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
        redisUtil.set(getCaptchaKey(uuid), result, captchaExpireSecond, java.util.concurrent.TimeUnit.SECONDS);
        return new CaptchaEntity(uuid, captcha.toBase64());
    }


}
