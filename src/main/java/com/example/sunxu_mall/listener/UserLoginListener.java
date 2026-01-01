package com.example.sunxu_mall.listener;

import com.example.sunxu_mall.dto.ip.IpCityDTO;
import com.example.sunxu_mall.event.UserLoginEvent;
import com.example.sunxu_mall.service.IpCityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户登录事件监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLoginListener {

    private final IpCityService ipCityService;

    @Async("loginEventExecutor")
    @EventListener
    public void handleUserLogin(UserLoginEvent event) {
        long start = System.currentTimeMillis();
        
        try {
            // 1. 查询IP地理位置 (走多级缓存+离线库)
            IpCityDTO ipCityDTO = ipCityService.getCityByIp(event.getIp());
            
            // 2. 打印详细审计日志
            String location = "Unknown";
            if (ipCityDTO != null) {
                location = String.format("%s/%s", ipCityDTO.getProvince(), ipCityDTO.getCity());
            }
            
            long cost = System.currentTimeMillis() - start;
            log.info("[LoginAudit] User={}({}) IP={} Location={} Cost={}ms", 
                    event.getUsername(), 
                    event.getUserId(),
                    event.getIp(), 
                    location,
                    cost);
            
            // 3. 可以在此处扩展其他逻辑，如发送邮件、风控检查等
            
        } catch (Exception e) {
            log.error("[LoginAudit] Failed to process login event for user {}", event.getUsername(), e);
        }
    }
}
