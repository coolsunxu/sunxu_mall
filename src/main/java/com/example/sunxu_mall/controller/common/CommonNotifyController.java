package com.example.sunxu_mall.controller.common;

import com.example.sunxu_mall.context.AuditUser;
import com.example.sunxu_mall.context.AuditUserProvider;
import com.example.sunxu_mall.service.common.CommonNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知管理 Controller
 *
 * @author sunxu
 * @version 1.0
 * @date 2026/2/1
 */
@Slf4j
@Tag(name = "通知管理", description = "通知相关接口")
@RestController
@RequestMapping("/v1/notify")
public class CommonNotifyController {

    private final CommonNotifyService commonNotifyService;

    public CommonNotifyController(CommonNotifyService commonNotifyService) {
        this.commonNotifyService = commonNotifyService;
    }

    /**
     * 标记通知为已读
     *
     * @param notifyId 通知ID
     */
    @Operation(summary = "标记通知为已读", description = "用户阅读通知后调用此接口标记为已读状态")
    @PostMapping("/{notifyId}/read")
    public void markAsRead(
            @Parameter(description = "通知ID", required = true)
            @PathVariable Long notifyId
    ) {
        log.info("Mark notify as read, notifyId={}", notifyId);
        commonNotifyService.markAsRead(notifyId);
    }

    /**
     * 标记所有通知为已读
     */
    @Operation(summary = "标记所有通知为已读", description = "用户点击通知中心后调用此接口，将所有未读通知标记为已读")
    @PostMapping("/read-all")
    public int markAllAsRead() {
        AuditUser currentUser = AuditUserProvider.getCurrentUserOrNull();
        Long userId = currentUser.getUserId();
        log.info("Mark all notify as read, userId={}", userId);
        return commonNotifyService.markAllAsRead(userId);
    }
}
