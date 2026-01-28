package com.example.sunxu_mall.aspect;

import com.example.sunxu_mall.util.FillUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * MyBatis 拦截器，自动填充审计字段
 *
 * @author sunxu
 * @version 1.0
 */
@Slf4j
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class AuditInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        // 参数为空则跳过
        if (Objects.isNull(parameter)) {
            return invocation.proceed();
        }

        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        if (sqlCommandType == SqlCommandType.INSERT) {
            fillForInsert(parameter);
        } else if (sqlCommandType == SqlCommandType.UPDATE) {
            fillForUpdate(parameter);
        }

        return invocation.proceed();
    }

    private void fillForInsert(Object parameter) {
        Object target = unwrapTarget(parameter);
        if (Objects.nonNull(target)) {
            FillUserUtil.fillInsert(target);
        }
    }

    private void fillForUpdate(Object parameter) {
        Object target = unwrapTarget(parameter);
        if (Objects.nonNull(target)) {
            FillUserUtil.fillUpdate(target);
        }
    }

    private Object unwrapTarget(Object parameter) {
        if (parameter instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) parameter;
            // 注意：MyBatis 的 MapperMethod$ParamMap 对不存在的 key 调 get() 会抛 BindingException
            // 因此这里必须先 containsKey 再 get
            String[] keys = new String[]{"row", "record", "et", "entity", "param1"};
            for (String key : keys) {
                if (map.containsKey(key)) {
                    Object val = map.get(key);
                    if (Objects.nonNull(val)) {
                        return val;
                    }
                }
            }
            return null;
        }
        return parameter;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
