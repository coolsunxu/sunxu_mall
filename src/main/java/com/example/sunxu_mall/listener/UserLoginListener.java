package com.example.sunxu_mall.listener;

import com.alibaba.fastjson.JSON;
import com.example.sunxu_mall.dto.ip.IpCityDTO;
import com.example.sunxu_mall.entity.common.CommonTaskEntity;
import com.example.sunxu_mall.entity.email.RemoteLoginEmailEntity;
import com.example.sunxu_mall.entity.sys.web.UserWebEntity;
import com.example.sunxu_mall.enums.EmailTypeEnum;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.enums.TaskStatusEnum;
import com.example.sunxu_mall.enums.TaskTypeEnum;
import com.example.sunxu_mall.event.UserLoginEvent;
import com.example.sunxu_mall.service.IpCityService;
import com.example.sunxu_mall.service.common.CommonTaskService;
import com.example.sunxu_mall.service.sys.UserService;
import com.example.sunxu_mall.validator.LoginValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 用户登录事件监听器
 */
@Slf4j
@Component
public class UserLoginListener {

    private final IpCityService ipCityService;
    private final UserService userService;
    private final CommonTaskService commonTaskService;

    public UserLoginListener(
            IpCityService ipCityService,
            UserService userService,
            CommonTaskService commonTaskService
    ) {
        this.ipCityService = ipCityService;
        this.userService = userService;
        this.commonTaskService = commonTaskService;
    }

    @Async("loginEventExecutor")
    @EventListener(condition = "#event.getIp()!=null")
    public void handleUserLogin(UserLoginEvent event) {
        
        try {
            // 查询IP地理位置 (走多级缓存+离线库)
            IpCityDTO ipCityDTO = ipCityService.getCityByIp(event.getIp());

            // 更新用户最后登录城市
            updateSysUser(event, ipCityDTO);

            // 执行异地登录验证 (静态方法调用)
            if (LoginValidator.checkGeoLocation(event, ipCityDTO)) {
                log.warn("[recordRemoteLogin] -> user: {} last_login_city: {} current_city: {}", event.getUsername(), event.getCity(), ipCityDTO.getCity());
                // 记录待办项(common_task)+更新用户表(sys_user)
                recordRemoteLogin(event, ipCityDTO);
            }
            
        } catch (Exception e) {
            log.warn("[LoginAudit] Failed to process login event for user {}", event.getUsername(), e);
        }
    }

    // 记录异地登录
    private void recordRemoteLogin(UserLoginEvent event, IpCityDTO ipCityDTO) {
        insertCommonTask(event, ipCityDTO);
    }

    private void insertCommonTask(UserLoginEvent event, IpCityDTO ipCityDTO) {

        // 构造参数详情
        RemoteLoginEmailEntity email = RemoteLoginEmailEntity.builder()
                .email("")
                .username(event.getUsername())
                .nickName("")
                .cityName(ipCityDTO.getCity())
                .loginTime(String.valueOf(event.getLoginTime()))
                .device("")
                .ip(event.getIp())
                .build();

        // 插入 CommonTask
        CommonTaskEntity task = CommonTaskEntity.builder()
                .name(EmailTypeEnum.REMOTE_LOGIN.getDesc())
                .type(TaskTypeEnum.SEND_EMAIL.getCode())
                .status(TaskStatusEnum.WAITING.getCode())
                .bizType(ExcelBizTypeEnum.USER.getCode())
                .createTime(java.time.LocalDateTime.now())
                .createUserId(event.getUserId())
                .createUserName(event.getUsername())
                .isDel(false)
                .requestParam(JSON.toJSONString(email))
                .build();

        commonTaskService.insert(task);
    }

    private void updateSysUser(UserLoginEvent event, IpCityDTO ipCityDTO) {

        if (Objects.isNull(ipCityDTO) || !StringUtils.hasText(ipCityDTO.getCity())) {
            return;
        }

        // 更新用户最新的登录位置和时间 (不更新IP字段)
        UserWebEntity updateUser = UserWebEntity.builder()
                .id(event.getUserId())
                .lastLoginCity(ipCityDTO.getCity())
                .lastLoginTime(event.getLoginTime())
                .build();

        userService.update(updateUser);
    }
}
