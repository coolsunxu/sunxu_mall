package com.example.sunxu_mall.aspect;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import com.example.sunxu_mall.annotation.ExcelExport;
import com.example.sunxu_mall.constant.MQConstant;
import com.example.sunxu_mall.dto.common.CommonTaskRequestDTO;
import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.idempotency.IdempotencyProperties;
import com.example.sunxu_mall.mq.producer.MessageProducer;
import com.example.sunxu_mall.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * 通用任务切面：拦截 @ExcelExport 注解方法，生成幂等 dedupKey 并发送异步创建任务消息
 *
 * @author sunxu
 */
@Slf4j
@Aspect
@Component
public class CommonTaskAspect {

    private final MessageProducer messageProducer;
    private final IdempotencyProperties idempotencyProperties;

    public CommonTaskAspect(MessageProducer messageProducer,
                            IdempotencyProperties idempotencyProperties) {
        this.messageProducer = messageProducer;
        this.idempotencyProperties = idempotencyProperties;
    }

    @Pointcut("@annotation(com.example.sunxu_mall.annotation.ExcelExport)")
    public void pointcut() {
    }

    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ExcelExport excelExport = method.getAnnotation(ExcelExport.class);

        if (Objects.isNull(excelExport)) {
            return;
        }

        ExcelBizTypeEnum excelBizTypeEnum = excelExport.value();
        String paramJson = extractParamJson(joinPoint);
        Long userId = null;
        String userName = null;

        // 填充用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!Objects.isNull(authentication) && authentication.getPrincipal() instanceof JwtUserEntity) {
            JwtUserEntity user = (JwtUserEntity) authentication.getPrincipal();
            userId = user.getId();
            userName = user.getUsername();
        }

        String fingerprint = resolveFingerprint(userId, excelBizTypeEnum, paramJson);
        String requestKey = buildRequestKey(fingerprint);

        CommonTaskRequestDTO dto = CommonTaskRequestDTO.builder()
                .dedupKey(requestKey)
                .fingerprint(fingerprint)
                .bizType(excelBizTypeEnum)
                .paramJson(paramJson)
                .userId(userId)
                .userName(userName)
                .build();

        // 使用实例级 requestKey 作为 MQ msg_key，避免历史 Outbox 记录阻塞新任务
        messageProducer.send(
                MQConstant.MALL_COMMON_TASK_CREATE_TOPIC,
                MQConstant.TAG_EXCEL_EXPORT_CREATE,
                requestKey,
                JsonUtil.toJsonStr(dto)
        );
    }

    /**
     * 提取方法第一个参数的 JSON 字符串
     */
    private String extractParamJson(JoinPoint joinPoint) {
        Object[] arguments = joinPoint.getArgs();
        if (ArrayUtil.isNotEmpty(arguments)) {
            return JsonUtil.toJsonStr(arguments[0]);
        }
        return "";
    }

    /**
     * 解析幂等 dedupKey
     * <p>
     * 优先从 HTTP Header 获取 Idempotency-Key，
     * 若未传则基于 userId + bizType + paramJson 做 MD5 摘要
     */
    private String resolveFingerprint(Long userId, ExcelBizTypeEnum bizType, String paramJson) {
        // 尝试从 HTTP Header 获取
        String headerKey = getIdempotencyHeaderValue();
        if (StringUtils.isNotBlank(headerKey)) {
            return md5Hex(headerKey);
        }

        // Fallback：确定性摘要
        String raw = (!Objects.isNull(userId) ? userId : "0")
                + ":" + bizType.getCode()
                + ":" + (StringUtils.isNotBlank(paramJson) ? paramJson : "");
        return md5Hex(raw);
    }

    /**
     * 构建本次请求唯一键，兼顾追踪与 Outbox 去重隔离。
     */
    private String buildRequestKey(String fingerprint) {
        return "task:" + fingerprint + ":" + IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * 从当前 HTTP 请求中获取幂等 Header
     */
    private String getIdempotencyHeaderValue() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (!Objects.isNull(attrs)) {
                HttpServletRequest request = attrs.getRequest();
                return request.getHeader(idempotencyProperties.getHeaderName());
            }
        } catch (Exception e) {
            log.debug("Failed to get Idempotency-Key header", e);
        }
        return null;
    }

    /**
     * 计算 MD5 十六进制摘要
     */
    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }
}
