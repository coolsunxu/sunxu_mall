package com.example.sunxu_mall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
@Schema(description = "User Creation DTO")
public class UserCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Username", required = true)
    @NotBlank(message = "Username cannot be empty")
    @Size(max = 30, message = "Username cannot exceed 30 characters")
    private String userName;

    @Schema(description = "Password", required = true)
    @NotBlank(message = "Password cannot be empty")
    @Size(max = 200, message = "Password cannot exceed 200 characters")
    private String password;

    @Schema(description = "Email")
    @Email(message = "Email format is invalid")
    @Size(max = 50, message = "Email cannot exceed 50 characters")
    private String email;

    @Schema(description = "Phone number")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @Schema(description = "Department ID")
    private Long deptId;

    @Schema(description = "Job ID")
    private Long jobId;

    @Schema(description = "Nickname")
    @Size(max = 30, message = "Nickname cannot exceed 30 characters")
    private String nickName;

    @Schema(description = "Sex: 1-Male, 2-Female")
    private Byte sex;

    @Schema(description = "Valid status: 1-Valid, 0-Invalid")
    private Boolean validStatus;

    @Schema(description = "Avatar ID")
    private Long avatarId;
}
