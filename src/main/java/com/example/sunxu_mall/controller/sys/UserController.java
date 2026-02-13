package com.example.sunxu_mall.controller.sys;

import com.example.sunxu_mall.annotation.ExcelExport;
import com.example.sunxu_mall.annotation.Idempotency;
import com.example.sunxu_mall.convert.user.UserStructMapper;
import com.example.sunxu_mall.dto.user.UserCreateDTO;
import com.example.sunxu_mall.dto.user.UserQueryDTO;
import com.example.sunxu_mall.dto.user.UserUpdateDTO;
import com.example.sunxu_mall.entity.sys.web.UserWebEntity;
import com.example.sunxu_mall.enums.ExcelBizTypeEnum;
import com.example.sunxu_mall.model.ResponseCursorEntity;
import com.example.sunxu_mall.service.sys.UserService;
import com.example.sunxu_mall.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/2 13:49
 * @description User Controller
 */


@Slf4j
@Tag(name = "用户操作", description = "用户接口")
@RestController
@RequestMapping("/v1/user")
public class UserController {

    private final UserService userService;
    private final UserStructMapper userStructMapper;

    public UserController(
            UserService userService,
            UserStructMapper userStructMapper
    ) {
        this.userService = userService;
        this.userStructMapper = userStructMapper;
    }

    /**
     * 双向游标分页查询用户列表（支持指定页码）
     * 注意：已移除 searchByPage 和 searchByCursor，统一使用此接口
     */
    @Operation(summary = "查询用户列表（游标分页）", description = "支持向前/向后翻页，可指定页码")
    @PostMapping("/searchByBidirectionalCursor")
    public ResponseCursorEntity<UserVO> searchByBidirectionalCursor(@RequestBody UserQueryDTO userQueryDTO) {
        ResponseCursorEntity<UserWebEntity> cursorEntity = userService.searchByBidirectionalCursor(userQueryDTO);
        return ResponseCursorEntity.convert(cursorEntity, userStructMapper::toVOList);
    }


    /**
     * 添加用户
     *
     * @param userCreateDTO 用户实体
     */
    @Operation(summary = "添加用户", description = "添加用户")
    @Idempotency
    @PostMapping("/insert")
    public void insert(@RequestBody @Valid UserCreateDTO userCreateDTO) {
        UserWebEntity userEntity = userStructMapper.toEntity(userCreateDTO);
        log.info("[insert] -> insert user info {}", userEntity);
        userService.insert(userEntity);
    }

    /**
     * 修改用户
     *
     * @param userUpdateDTO 用户实体
     * @return 影响行数
     */
    @Operation(summary = "修改用户", description = "修改用户")
    @Idempotency
    @PostMapping("/update")
    public int update(@RequestBody @Valid UserUpdateDTO userUpdateDTO) {
        UserWebEntity userEntity = userStructMapper.toEntity(userUpdateDTO);
        return userService.update(userEntity);
    }

    /**
     * 删除用户
     *
     * @param ids 用户ID
     * @return 影响行数
     */
    @Operation(summary = "删除用户", description = "删除用户")
    @Idempotency
    @PostMapping("/deleteByIds")
    public int deleteById(@RequestBody @NotNull List<Long> ids) {
        return userService.deleteByIds(ids);
    }


    /**
     * 重置密码
     *
     * @param ids 用户ID
     * @return 影响行数
     */
    @Operation(summary = "重置密码", description = "重置密码")
    @Idempotency
    @PostMapping("/resetPwd")
    public int resetPwd(@RequestBody @NotNull List<Long> ids) {
        return userService.resetPwd(ids);
    }

    /**
     * 导出用户数据, 通过注解异步实现
     */
    @ExcelExport(ExcelBizTypeEnum.USER)
    @Operation(summary = "导出用户数据", description = "导出用户数据")
    @Idempotency(ttlSeconds = 30)
    @PostMapping("/export")
    public void export(@RequestBody UserQueryDTO userQueryDTO) {
        // Method body is empty as logic is handled by Aspect
    }
}
