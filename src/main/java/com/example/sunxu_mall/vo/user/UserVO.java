package com.example.sunxu_mall.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "User VO")
public class UserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "User ID")
    private Long id;

    @Schema(description = "Username")
    private String userName;

    @Schema(description = "Nickname")
    private String nickName;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Department ID")
    private Long deptId;

    @Schema(description = "Job ID")
    private Long jobId;

    @Schema(description = "Sex")
    private Byte sex;

    @Schema(description = "Valid status")
    private Boolean validStatus;

    @Schema(description = "Avatar ID")
    private Long avatarId;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Last login time")
    private LocalDateTime lastLoginTime;

    @Schema(description = "Last login city")
    private String lastLoginCity;
}
