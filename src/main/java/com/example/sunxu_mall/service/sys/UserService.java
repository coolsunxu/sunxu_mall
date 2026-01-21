package com.example.sunxu_mall.service.sys;

import cn.hutool.core.util.IdUtil;
import com.example.sunxu_mall.dto.user.UserQueryDTO;
import com.example.sunxu_mall.entity.auth.AuthUserEntity;
import com.example.sunxu_mall.entity.auth.CaptchaEntity;
import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.entity.auth.TokenEntity;
import com.example.sunxu_mall.entity.sys.web.UserWebEntity;
import com.example.sunxu_mall.entity.sys.web.UserWebEntityExample;
import com.example.sunxu_mall.event.UserLoginEvent;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.helper.TokenHelper;
import com.example.sunxu_mall.mapper.sys.UserWebEntityMapper;
import com.example.sunxu_mall.service.BaseService;
import com.example.sunxu_mall.util.*;
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

/**
 * @author sunxu
 */

@Slf4j
@Service
public class UserService extends BaseService<UserWebEntity, UserQueryDTO> {
    private final UserWebEntityMapper userMapper;
    private final RedisUtil redisUtil;
    private final PasswordUtil passwordUtil;
    private final AuthenticationManager authenticationManager;
    private final TokenHelper tokenHelper;
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
            ApplicationEventPublisher eventPublisher
    ) {
        this.userMapper = userMapper;
        this.redisUtil = redisUtil;
        this.passwordUtil = passwordUtil;
        this.authenticationManager = authenticationManager;
        this.tokenHelper = tokenHelper;
        this.eventPublisher = eventPublisher;
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

    /**
     * 根据 id查询用户信息
     *
     * @param id 用户 id
     * @return 返回用户信息
     */
    public UserWebEntity findById(Long id) {
        return userMapper.selectByPrimaryKey(id);
    }

    @Override
    protected List<UserWebEntity> selectListWithLimit(UserQueryDTO queryDTO, int limit) {
        return userMapper.selectListWithLimit(
                queryDTO.getUserName(),
                queryDTO.getPhone(),
                queryDTO.getEmail(),
                queryDTO.getValidStatus(),
                queryDTO.getDeptId(),
                limit
        );
    }

    @Override
    protected List<UserWebEntity> selectListByCursorWithLimit(UserQueryDTO queryDTO, Long cursorId, int limit) {
        return userMapper.selectByCursorWithLimit(
                queryDTO.getUserName(),
                queryDTO.getPhone(),
                queryDTO.getEmail(),
                queryDTO.getValidStatus(),
                queryDTO.getDeptId(),
                cursorId,
                limit
        );
    }

    @Override
    protected Long extractEntityId(UserWebEntity entity) {
        return Objects.isNull(entity) ? null : entity.getId();
    }

    /**
     * 添加用户
     *
     * @param userEntity 用户实体
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert(UserWebEntity userEntity) {
        if (StringUtils.isNotBlank(userEntity.getPassword())) {
            String plainPwd = passwordUtil.decodeRsaPassword(userEntity.getPassword());
            userEntity.setPassword(passwordUtil.encode(plainPwd));
        }
        JwtUserEntity currentUser = SecurityUtil.getUserInfo();
        userEntity.setCreateUserId(currentUser.getId());
        userEntity.setCreateUserName(currentUser.getUsername());
        userEntity.setUpdateUserId(currentUser.getId());
        userEntity.setUpdateUserName(currentUser.getUsername());
        userEntity.setCreateTime(LocalDateTime.now());
        userEntity.setUpdateTime(LocalDateTime.now());
        userEntity.setIsDel(false);
        userEntity.setVersion(1);
        userMapper.insertSelective(userEntity);
    }

    /**
     * 更新用户信息
     * 采用乐观锁机制：version + retry
     *
     * @param userEntity 用户实体
     * @return 更新行数
     */
    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public int update(UserWebEntity userEntity) {
        if (Objects.isNull(userEntity) || Objects.isNull(userEntity.getId())) {
            throw new BusinessException(PARAMETER_MISSING.getCode(), "User ID cannot be null");
        }

        Long userId = userEntity.getId();
        UserWebEntity current = userMapper.selectByPrimaryKey(userId);
        if (Objects.isNull(current)) {
            throw new BusinessException(USER_NOT_EXIST.getCode(), USER_NOT_EXIST.getMessage());
        }

        Integer oldVersion = current.getVersion();
        BeanCopyUtils.copyNonNullProperties(userEntity, current);

        current.setVersion(oldVersion);
        current.setUpdateTime(LocalDateTime.now());

        int rows = userMapper.updateUserInfoWithVersion(current);
        if (rows == 0) {
            log.warn("Optimistic lock failure for userId {}", userId);
            throw new OptimisticLockingFailureException("User data has been modified, please retry");
        }
        return rows;
    }

    /**
     * 批量删除用户（逻辑删除）
     *
     * @param ids 用户ID列表
     * @return 更新行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(List<Long> ids) {
        UserWebEntityExample example = new UserWebEntityExample();
        example.createCriteria().andIdIn(ids);
        UserWebEntity update = new UserWebEntity();
        update.setIsDel(true);
        update.setUpdateTime(LocalDateTime.now());
        return userMapper.updateByExampleSelective(update, example);
    }

    /**
     * 批量重置密码
     *
     * @param ids 用户ID列表
     * @return 更新行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int resetPwd(List<Long> ids) {
        UserWebEntityExample example = new UserWebEntityExample();
        example.createCriteria().andIdIn(ids);
        UserWebEntity update = new UserWebEntity();
        // Default password logic here, assumed 123456
        update.setPassword(passwordUtil.encode("123456"));
        update.setUpdateTime(LocalDateTime.now());
        return userMapper.updateByExampleSelective(update, example);
    }
}
