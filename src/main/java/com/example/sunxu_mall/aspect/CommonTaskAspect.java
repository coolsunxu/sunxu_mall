package com.example.sunxu_mall.aspect;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.example.sunxu_mall.annotation.ExcelExport;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.enums.TaskStatusEnum;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.service.common.CommonTaskService;
import com.example.sunxu_mall.util.FillUserUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
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

    private final CommonTaskService commonTaskService;

    public CommonTaskAspect(CommonTaskService commonTaskService) {
        this.commonTaskService = commonTaskService;
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

            CommonTaskEntity commonTaskEntity = createCommonTaskEntity(excelBizTypeEnum);
            Object[] arguments = joinPoint.getArgs();
            if (ArrayUtil.isNotEmpty(arguments)) {
                Object requestParam = arguments[0];
                commonTaskEntity.setRequestParam(JSONUtil.toJsonStr(requestParam));
            }

            commonTaskService.insert(commonTaskEntity);
        }
    }


    private CommonTaskEntity createCommonTaskEntity(ExcelBizTypeEnum excelBizTypeEnum) {
        CommonTaskEntity commonTaskEntity = new CommonTaskEntity();
        commonTaskEntity.setName(getTaskName(excelBizTypeEnum));
        commonTaskEntity.setStatus(TaskStatusEnum.WAITING.getCode());
        commonTaskEntity.setFailureCount((byte) 0);
        commonTaskEntity.setType(TaskTypeEnum.EXPORT_EXCEL.getCode());
        commonTaskEntity.setBizType(excelBizTypeEnum.getCode());
        FillUserUtil.fillCreateUserInfo(commonTaskEntity);
        return commonTaskEntity;
    }

    private String getTaskName(ExcelBizTypeEnum excelBizTypeEnum) {
        return String.format("导出%s数据", excelBizTypeEnum.getDesc());
    }
}
