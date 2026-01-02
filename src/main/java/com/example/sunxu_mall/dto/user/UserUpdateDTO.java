package com.example.sunxu_mall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "User Update DTO")
public class UserUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "User ID", required = true)
    @NotNull(message = "User ID cannot be null")
    private Long id;

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

}
