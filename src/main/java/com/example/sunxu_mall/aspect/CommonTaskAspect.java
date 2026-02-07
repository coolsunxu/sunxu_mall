package com.example.sunxu_mall.aspect;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import com.example.sunxu_mall.util.JsonUtil;
import com.example.sunxu_mall.annotation.ExcelExport;
import com.example.sunxu_mall.constant.MQConstant;
import com.example.sunxu_mall.dto.common.CommonTaskRequestDTO;
import com.example.sunxu_mall.entity.auth.JwtUserEntity;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.mq.producer.MessageProducer;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/9 20:19
 * @description
 */

@Aspect
@Component
public class CommonTaskAspect {

    private final MessageProducer messageProducer;

    public CommonTaskAspect(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @Pointcut("@annotation(com.example.sunxu_mall.annotation.ExcelExport)")
    public void pointcut() {
    }

    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ExcelExport excelExport = method.getAnnotation(ExcelExport.class);

        if (excelExport != null) {
            ExcelBizTypeEnum excelBizTypeEnum = excelExport.value();

            CommonTaskRequestDTO.CommonTaskRequestDTOBuilder builder = CommonTaskRequestDTO.builder()
                    .bizType(excelBizTypeEnum);

            Object[] arguments = joinPoint.getArgs();
            if (ArrayUtil.isNotEmpty(arguments)) {
                Object requestParam = arguments[0];
                builder.paramJson(JsonUtil.toJsonStr(requestParam));
            }

            // 填充用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof JwtUserEntity) {
                JwtUserEntity user = (JwtUserEntity) authentication.getPrincipal();
                builder.userId(user.getId())
                        .userName(user.getUsername());
            }

            CommonTaskRequestDTO dto = builder.build();

            // 发送异步创建请求
            messageProducer.send(
                    MQConstant.MALL_COMMON_TASK_CREATE_TOPIC,
                    MQConstant.TAG_EXCEL_EXPORT_CREATE,
                    IdUtil.getSnowflakeNextIdStr(),
                    JsonUtil.toJsonStr(dto)
            );
        }
    }
}
